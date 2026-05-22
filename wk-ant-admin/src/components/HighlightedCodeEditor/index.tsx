import Editor from 'react-simple-code-editor';
import Prism from 'prismjs';
import 'prismjs/components/prism-clike';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-json';
import 'prismjs/themes/prism.css';
import * as React from 'react';

type HighlightedCodeEditorProps = {
  value?: string;
  onChange?: (value: string) => void;
  language?: 'javascript' | 'json' | 'text';
  minHeight?: number;
  readOnly?: boolean;
};

const languageMap: Record<NonNullable<HighlightedCodeEditorProps['language']>, string> = {
  javascript: 'javascript',
  json: 'json',
  text: 'clike',
};

const HighlightedCodeEditor: React.FC<HighlightedCodeEditorProps> = ({
  value = '',
  onChange,
  language = 'javascript',
  minHeight = 240,
  readOnly = false,
}) => (
  <div
    style={{
      border: '1px solid var(--ant-color-border)',
      borderRadius: 8,
      background: 'var(--ant-color-bg-container)',
      overflow: 'hidden',
    }}
  >
    <Editor
      value={value}
      onValueChange={readOnly ? () => undefined : (next) => onChange?.(next)}
      highlight={(code) =>
        Prism.highlight(code, Prism.languages[languageMap[language]], languageMap[language])
      }
      padding={16}
      readOnly={readOnly}
      style={{
        fontFamily:
          '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace',
        fontSize: 13,
        minHeight,
        background: 'transparent',
      }}
      textareaClassName="budiot-highlighted-code-editor"
      preClassName="budiot-highlighted-code-editor__pre"
    />
  </div>
);

export default HighlightedCodeEditor;

/**
 * 这个文件作为组件的目录
 * 目的是统一管理对外输出的组件，方便分类
 */
/**
 * 布局组件
 */
import Footer from './Footer';
import NoticeDropdown from './RightContent/NoticeDropdown';
import AppSwitcher from './RightContent/AppSwitcher';
import { SelectLang } from './RightContent';
import { AvatarDropdown, AvatarName } from './RightContent/AvatarDropdown';
import VideoPlayer from './VideoPlayer';
import VideoPlayerOverlay from './VideoPlayerOverlay';
export type { VideoPlayerOverlayDetection, VideoPlayerOverlayRegion } from './VideoPlayerOverlay';

export { AppSwitcher, AvatarDropdown, AvatarName, Footer, NoticeDropdown, SelectLang, VideoPlayer, VideoPlayerOverlay };

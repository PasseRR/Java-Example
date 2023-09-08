import java from "./java/index";
import sun from "./sun/index";

const site = {
    main: 'https://www.xiehai.zone',
    logo: '/logo.svg',
    // 标题
    title: 'JDK8源码阅读',
    // 描述
    description: 'JDK8源码阅读',
    // github仓库
    repository: 'Java-Example',
    // 主分支
    branch: 'master',
    // 基础路径
    base: '/Java-Example',
    // google 分析
    google: 'G-1L1DPX3PFD',
    // 百度统计
    baidu: 'f6a0d2cc07d505a22b5a318eee45a715',
    // 排除文件
    excludes: ["_chapters/*"],
    ignoreDeadLinks: []
}

function sidebars() {
    return {
        ...sun.bars(),
        ...java.bars(),
    };
}

function navs() {
    return [sun.nav(), java.nav()];
}

export {site, sidebars, navs};

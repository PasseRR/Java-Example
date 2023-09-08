import lang from "./lang";
import util from "./util";

function bars() {
    return {
        ...lang.bars(),
        ...util.bars(),
    }
}

function nav() {
    return {
        text: 'java',
        items: [lang.nav(), util.nav()]
    }
}

export default {bars, nav}

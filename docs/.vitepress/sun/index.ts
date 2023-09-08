import misc from "./misc";

function bars() {
    return {
        ...misc.bars(),
    }
}

function nav() {
    return {
        text: 'sun',
        items: [misc.nav()]
    }
}

export default {bars, nav}

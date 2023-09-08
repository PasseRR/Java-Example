function bars() {
    return {
        '/java/lang': {
            base: '/java/lang',
            items: [{
                text: 'Thread', link: '/Thread'
            }]
        }
    }
}

function nav() {
    return {
        text: 'lang',
        link: '/java/lang/index',
        activeMatch: '/java/lang/'
    }
}

export default {bars, nav}

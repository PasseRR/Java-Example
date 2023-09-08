function bars() {
    return {
        '/sun/misc': {
            base: '/sun/misc',
            items: [
                {text: 'Unsafe', link: '/Unsafe'}
            ]
        }
    }
}

function nav() {
    return {
        text: 'misc',
        link: '/sun/misc/package',
        activeMatch: '/sun/misc/'
    }
}

export default {bars, nav}

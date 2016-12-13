var strimBagVersion = '2.0.0';
var strimApi,
    strimFFZ;

function initStrimBag(attempts) {
    if (window.FrankerFaceZ !== undefined && window.jQuery !== undefined && window.App !== undefined) {
        strimFFZ = FrankerFaceZ.get();
        strimApi = strimFFZ.api('StrimBagZ', '', strimBagVersion);
        strimApi.log('Injected successfully.');
        addDevBadge();

        /* Fix chat scrolling */
        $(window).bind('resize', function() { ffz._roomv._scrollToBottom(); });
        /* ffz._roomv._$chatMessagesScroller.unbind('scroll') */
    } else {
    attempts = (attempts || 0) + 1;
    if (attempts < 60)
        return setTimeout(initStrimBag.bind(this, attempts), 1000);
      console.log('StrimBagZ: Could not find FFZ. Injection unsuccessful.');
    }
}

function addDevBadge() {
    strimApi.add_badge('strimbagz', {
        name: 'strimbagz',
        title: 'StrimBagZ Developer',
        image: 'https://i.imgur.com/sLYh6rk.png',
        alpha_image: 'https://i.imgur.com/59LFBS1.png',
        color: '#5f83a7'
    });
    strimApi.user_add_badge('luigitus', 21, 'strimbagz');
}

initStrimBag();
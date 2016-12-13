function twitchSetup() {
var checkSetup = window.localStorage.getItem('twitchSetup');
	if (checkSetup === undefined || checkSetup === null) {
		window.localStorage.setItem('twitchSetup', 'true');
		window.localStorage.setItem('Notifications:Dismissed', '[\"205\",\"205\",\"206\",\"207\"]');
		window.localStorage.setItem('euCookieDismiss', 'true');
		window.localStorage.setItem('example_whisper_sent', 'true');
		window.localStorage.setItem('TwitchCache:chatReplay', '{\"resource\":{\"hideNotice\":true},\"time\":1456300541280}');
	}
}

twitchSetup();
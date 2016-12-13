function firstSetup() {
var checkSetup = window.localStorage.getItem('firstSetup');
	if (checkSetup === undefined || checkSetup === null) {
		window.localStorage.setItem('firstSetup', 'true');
		window.localStorage.setItem('ffz_setting_chat_padding', 'true');
		window.localStorage.setItem('ffz_setting_channel_views', 'false');
		window.localStorage.setItem('ffz_setting_chat_rows', 'true');
		window.localStorage.setItem('ffz_setting_dark_twitch', 'true');
		window.localStorage.setItem('ffz_setting_dark_no_blue', 'true');
		window.localStorage.setItem('ffz_setting_follow_buttons', 'false');
		window.localStorage.setItem('ffz_setting_global_emotes_in_menu', 'true');
		window.localStorage.setItem('ffz_setting_link_info', 'false');
		window.localStorage.setItem('ffz_setting_minimal_chat', 'true');
		window.localStorage.setItem('ffz_setting_replace_twitch_menu', 'true');
		window.localStorage.setItem('ffz_setting_room_status', 'false');
		window.localStorage.setItem('ffz_setting_srl_races', 'false');
		window.localStorage.setItem('ffz_setting_stream_title', 'false');
		window.localStorage.setItem('TwitchCache:chatReplay', 'true');
	}
}

firstSetup();
function changeChatRoom(old, channel) {
    var Chat = FrankerFaceZ.utils.ember_lookup('controller:chat');
    var Room = FrankerFaceZ.utils.ember_resolve('model:room');

    if ( Chat.get('currentChannelRoom.id') !== old && Chat.get('currentRoom.id') === old ) {
        console.log('StrimBagZ Blur previous Chat room.');
        modifiedBlurRoom(Chat);
        console.log('StrimBagZ Blured room.');
    }

    if ( ! ffz.rooms[channel] || ! ffz.rooms[channel].room ) {
        console.log('StrimBagZ Find new chat room.');
        Room.findOne(channel);
        console.log('StrimBagZ Found new chat room.');
    }

    Chat.focusRoom(ffz.rooms[channel].room);

    console.log('StrimBagZ Focused new chat room.');
    if ( ffz.settings.pinned_rooms.indexOf(old) == -1 ) {
        var user = ffz.get_user();
        if ( ! user || user.login !== old ) {
            if ( Chat.get('currentChannelRoom.id') === old ) {
                console.log('StrimBagZ currentChannelRoom should not be destroyed.');
                return;
            }
            console.log('StrimBagZ Trying to remove old chat room.');
            ffz.remove_room(old);
            console.log('StrimBagZ Removed old chat room.');
        }
    }
}

function sbzChangeCurrentChannel(old, channel) {
    var Chat = FrankerFaceZ.utils.ember_lookup('controller:chat');
    var Room = FrankerFaceZ.utils.ember_resolve('model:room');

    if ( ! ffz.rooms[channel] || ! ffz.rooms[channel].room ) {
        console.log('StrimBagZ Find new chat room.');
        Room.findOne(channel);
        console.log('StrimBagZ Found new chat room.');
    }

    modifiedSetCurrentChannelRoom(Chat, ffz.rooms[channel].room);
}

function modifiedBlurRoom(Chat) {
    Chat.set('currentRoom', null);
}

function modifiedFocusRoom(Chat, room) {
    Chat.set('currentRoom', room);
}

function modifiedSetCurrentChannelRoom(Chat, room) {
    modifiedRemoveCurrentChannelRoom(Chat);
    Chat.set('currentChannelRoom', room);
    modifiedFocusRoom(Chat, room);
}

function modifiedRemoveCurrentChannelRoom(Chat) {
    var room = Chat.get('currentChannelRoom'),
    room_id = room && room.get('id'),
    user = ffz.get_user();
    if ( !((ffz._chatv && ffz._chatv._ffz_host === room_id) || (ffz.settings.pinned_rooms && ffz.settings.pinned_rooms.indexOf(room_id) !== -1)) ) {
        if ( room === Chat.get('currentRoom') )
            modifiedBlurRoom(Chat);
            if ( room && user && user.login !== room_id )
                room.ffzScheduleDestroy();
    }
    Chat.set('currentChannelRoom', void 0);
}



function getRoomArray() {
    var rooms = [];
    var rooms_dn = [];

    for (room_id in ffz.rooms) {
        rooms.push(room_id);
    }
    for (index = 0; index < rooms.length; ++index) {
        rooms_dn.push(ffz.rooms[rooms[index]].display_name);
    }

    return rooms_dn;
}

function getRoomIDs() {
    var rooms = [];

    for (room_id in ffz.rooms) {
        rooms.push(room_id);
    }
    return rooms;
}

function host(user, target) {
	var conn;
	for(var room_id in ffz.rooms) {
		if ( ! ffz.rooms.hasOwnProperty(room_id) )
			continue;
		var r = ffz.rooms[room_id],
			c = r && r.room && r.room.tmiRoom && r.room.tmiRoom._getConnection();

		if ( c.isConnected ) {
			conn = c;
			break;
		}
	}

	if (conn) {
		conn._send('PRIVMSG #' + user +' :/host ' + target);
	}
}

function unhost(user) {
	var conn;
	for(var room_id in ffz.rooms) {
		if ( ! ffz.rooms.hasOwnProperty(room_id) )
			continue;
		var r = ffz.rooms[room_id],
			c = r && r.room && r.room.tmiRoom && r.room.tmiRoom._getConnection();

		if ( c.isConnected ) {
			conn = c;
			break;
		}
	}

	if (conn) {
		conn._send('PRIVMSG #' + user +' :/unhost');
	}
}

function do_authorize(data) {
	var conn;
	for(var room_id in ffz.rooms) {
		if ( ! ffz.rooms.hasOwnProperty(room_id) )
			continue;
		var r = ffz.rooms[room_id],
			c = r && r.room && r.room.tmiRoom && r.room.tmiRoom._getConnection();

		if ( c.isConnected ) {
			conn = c;
			break;
		}
	}

	if (conn) {
		conn._send('PRIVMSG #frankerfacezauthorizer :AUTH ' + data);
	}
}

function get_editor_status() {
    return new Promise(function(succeed,fail) {
    		var user = ffz.get_user();
    		if ( ! user || ! user.login )
    			return fail();

    		jQuery.get('/' + user.login + '/dashboard/permissions').done(function(data) {
    			try {
    				var dom = new DOMParser().parseFromString(data, 'text/html'),
    					links = dom.querySelectorAll('#editable .label');

    				ffz._editor_of = _.map(links, function(e) {
    					var href = e.getAttribute('href');
    					return href && href.substr(href.lastIndexOf('/') + 1);
    				});

    				succeed(ffz._editor_of);

    			} catch(err) {
    				ffz.error('Failed to parse User Editor State', err);
    				fail();
    			}

    		}).fail(function(e) {
    			ffz.error('Failed to load User Editor State', e);
    			fail();
    		});
    	});
}
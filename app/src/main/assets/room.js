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
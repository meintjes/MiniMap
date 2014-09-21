var GRID_SIZE = 5;
var NUM_TILES = GRID_SIZE * GRID_SIZE;

var DATA_MAP_BITMAPS = 10;

function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function makeRandomTiles() {
    var tiles = [];
    for (var i = 0; i < NUM_TILES; i++) {
        tiles.push(randInt(0, 2));
    }

    var ret = {};
    ret[DATA_MAP_BITMAPS] = tiles;
    return ret;
}

function sendTiles() {
    Pebble.sendAppMessage(makeRandomTiles(), function(e) {
        console.log("Sent tiles.");
    }, function(e) {
        console.log("Failed to send tiles.");
    });
}

Pebble.addEventListener("ready", function(e) {
    sendTiles();
    setInterval(sendTiles, 5000);
});

#include <pebble.h>

Window* window;
Layer* mapLayer;
Layer* userLayer;

#define DATA_MAP_BITS 10

#define GRID_SIZE 5
#define NUM_TILES 25
static bool tiles[NUM_TILES] = {};

void drawCircle(GContext* ctx, GPoint center, int radius) {
    graphics_context_set_fill_color(ctx, GColorWhite);
    graphics_fill_circle(ctx, center, radius);
}

void mapLayerUpdateCallback(Layer* layer, GContext* ctx) {
    GRect bounds = layer_get_bounds(layer);

    int16_t tileWidth = bounds.size.w / GRID_SIZE;
    int16_t tileHeight = bounds.size.h / GRID_SIZE;

    for (int row = 0; row < GRID_SIZE; row++) {
        for (int column = 0; column < GRID_SIZE; column++) {
            int index = row * GRID_SIZE + column;
            if (!tiles[index]) {
                continue;
            }

            GPoint origin = {
                (int16_t) (column * tileWidth),
                (int16_t) (row * tileHeight)
            };
            GSize size = {tileWidth, tileHeight};
            GRect tile = {origin, size};
            graphics_context_set_fill_color(ctx, GColorWhite);
            graphics_draw_rect(ctx, tile);
            graphics_fill_rect(ctx, tile, 0, GCornerNone);
        }
    }
}

void updateMap(DictionaryIterator* iter, void* ctx) {
    Tuple* mapBitsTuple = dict_find(iter, DATA_MAP_BITS);
    if (mapBitsTuple) {
        for (int i = 0; i < NUM_TILES; i++) {
            tiles[i] = (bool) mapBitsTuple->value->data[i];
        }
        layer_mark_dirty(mapLayer);
    }
}

void userLayerUpdateCallback(Layer* layer, GContext* ctx) {
    GRect bounds = layer_get_bounds(layer);

    GPoint center = {
        (int16_t) (bounds.size.w / 2),
        (int16_t) (bounds.size.h / 2),
    };

    const int RADIUS = 6;
    const int BORDER_THICKNESS = 2;

    graphics_context_set_fill_color(ctx, GColorWhite);
    graphics_fill_circle(ctx, center, RADIUS + 1);
    graphics_context_set_fill_color(ctx, GColorBlack);
    graphics_fill_circle(ctx, center, RADIUS);
    graphics_context_set_fill_color(ctx, GColorWhite);
    graphics_fill_circle(ctx, center, RADIUS - BORDER_THICKNESS);
}

void init(void) {
    window = window_create();
    window_stack_push(window, true);
    window_set_background_color(window, GColorBlack);

    Layer* rootLayer = window_get_root_layer(window);
    GRect frame = layer_get_frame(rootLayer);

    mapLayer = layer_create(frame);
    layer_set_update_proc(mapLayer, &mapLayerUpdateCallback);
    layer_add_child(rootLayer, mapLayer);

    userLayer = layer_create(frame);
    layer_set_update_proc(userLayer, &userLayerUpdateCallback);
    layer_add_child(rootLayer, userLayer);

    app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());
    app_message_register_inbox_received(&updateMap);
}

void shutdown(void) {
    layer_destroy(mapLayer);
    window_destroy(window);
}

int main(void) {
    init();
    app_event_loop();
    shutdown();
}

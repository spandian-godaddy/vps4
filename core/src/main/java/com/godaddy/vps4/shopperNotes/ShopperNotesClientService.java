package com.godaddy.vps4.shopperNotes;

import java.util.UUID;

public interface ShopperNotesClientService {
    UUID processShopperMessage(ShopperNoteRequest request);
}

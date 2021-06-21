package com.godaddy.vps4.shopperNotes;

import java.util.UUID;

public interface ShopperNotesService {
    UUID processShopperMessage(UUID vmId, String note);
}

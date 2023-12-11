package com.godaddy.vps4.productPackage;

import java.util.Set;

public interface PackageService {
    Set<String> getPackages(Integer... tiers);
}

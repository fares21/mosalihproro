#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Odoo Repair Manager - Activation Key Generator (Keygen)
======================================================
This script generates valid activation keys for the Android phone maintenance app.
It uses an XOR Cipher of combined Device ID, License Year, and a Fixed Secret Word, 
and encodes the result to Base64.

To run:
    python keygen.py
"""

import base64

# =========================================================================
# CONFIGURATION - MUST MATCH KOTLIN CODE EXACTLY
# Change these values to secure your license generator!
# =========================================================================
FIXED_SECRET_WORD = "ODOO_PHONE_REPAIR_2026"  # الكلمة السرية الثابتة للتطبيق
FIXED_XOR_KEY = "SECRET_XOR_KEY_99"          # المفتاح السري الثابت لتشفير XOR
# =========================================================================

def generate_activation_key(device_id: str, license_year: int) -> str:
    """
    Generates a secure activation key by combining device_id, license_year, 
    and secret word, executing XOR encryption, and encoding to Base64.
    """
    # 1. Combine inputs with a pipe separator
    combined_str = f"{device_id.strip()}|{license_year}|{FIXED_SECRET_WORD}"
    
    # 2. Convert to raw bytes (UTF-8)
    combined_bytes = combined_str.encode("utf-8")
    key_bytes = FIXED_XOR_KEY.encode("utf-8")
    
    # 3. Apply XOR Cipher on each byte
    encrypted_bytes = bytearray(len(combined_bytes))
    for i in range(len(combined_bytes)):
        # XOR current byte with corresponding key byte (circular rotation)
        encrypted_bytes[i] = combined_bytes[i] ^ key_bytes[i % len(key_bytes)]
        
    # 4. Convert the encrypted binary result to Base64 string
    base64_encoded_key = base64.b64encode(encrypted_bytes).decode("utf-8")
    
    return base64_encoded_key

def main():
    print("=========================================================")
    print("      Odoo Repair Manager - Activation Keygen            ")
    print("=========================================================")
    
    # User Input
    device_id = input("Enter Client DeviceID (Android ID): ").strip()
    if not device_id:
        print("[-] Error: Device ID cannot be empty.")
        return
        
    try:
        license_year = int(input("Enter License Expiry Year (e.g. 2026, 2027): ").strip())
    except ValueError:
        print("[-] Error: Expiry Year must be a valid number.")
        return
        
    # Generate Activation Key
    activation_key = generate_activation_key(device_id, license_year)
    
    print("\n------------------ GENERATED KEY ------------------")
    print(activation_key)
    print("---------------------------------------------------")
    print("[+] Provide this key to the customer for activation.")
    print("=========================================================")

if __name__ == "__main__":
    main()

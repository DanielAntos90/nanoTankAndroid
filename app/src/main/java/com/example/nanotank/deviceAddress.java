package com.example.nanotank;

public enum deviceAddress {
    NANOTANK("98:D3:91:FD:60:CB"),
    SMARTTANK("98:D3:32:F5:A0:D3");

    public final String address;

    private deviceAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return this.address;
    }
}
package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceUpdateRequest {

    @SerializedName("connect-public-key")
    var connectPublicKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("ep-grd-device-nickname")
    var deviceNickname: String? = null

    @SerializedName("ep-grd-device-uuid")
    var deviceUuid: String? = null

}
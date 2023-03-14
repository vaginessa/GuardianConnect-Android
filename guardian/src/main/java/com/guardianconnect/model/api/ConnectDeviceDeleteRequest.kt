package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceDeleteRequest {

    @SerializedName("connect-public-key")
    var connectPublicKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("device-uuid")
    var deviceUuid: String? = null
}
package com.ehealth.machine.utils

import com.tencent.videotwodemo_wangqing.app.App


inline fun toast(value: () -> String): Unit =
    App.ApplicationINSTANCE.toast(value)
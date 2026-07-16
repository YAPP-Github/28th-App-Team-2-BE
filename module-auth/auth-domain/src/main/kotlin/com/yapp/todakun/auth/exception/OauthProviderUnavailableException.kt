package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.BusinessException

class OauthProviderUnavailableException : BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)

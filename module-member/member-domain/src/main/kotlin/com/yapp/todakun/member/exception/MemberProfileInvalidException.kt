package com.yapp.todakun.member.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.member.code.MemberErrorCode

class MemberProfileInvalidException : BadRequestException(MemberErrorCode.MEMBER_PROFILE_INVALID)

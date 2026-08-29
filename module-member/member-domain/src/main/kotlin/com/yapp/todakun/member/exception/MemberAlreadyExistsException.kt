package com.yapp.todakun.member.exception

import com.yapp.todakun.common.exception.ConflictException
import com.yapp.todakun.member.code.MemberErrorCode

class MemberAlreadyExistsException : ConflictException(MemberErrorCode.MEMBER_ALREADY_EXISTS)

package com.yapp.todakun.member.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.member.code.MemberErrorCode

class MemberNotFoundException : NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND)

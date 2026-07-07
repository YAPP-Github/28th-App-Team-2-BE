package com.yapp.todakun.member.code

import com.yapp.todakun.common.exception.NotFoundException

class MemberNotFoundException : NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND)

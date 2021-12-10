1. 响应的时候，header可能一个key有多个值，但是注意，content-length不能这样！
2. 已有bug，servlet无法自己指定不等于body长度的content length

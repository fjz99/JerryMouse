1. 已有bug，servlet无法自己指定不等于body长度的content length
2. 响应的时候，header可能一个key有多个值，但是注意，content-length不能这样！
3. OutputStreamWriter自带了一个缓冲区，在close的时候会刷新缓冲区，所以要先close，再send Response，因为用户可能自己不flush， 而OutputStreamWriter会自动flush的

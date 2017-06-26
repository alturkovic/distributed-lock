if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1]) == 1
else
    return false
end
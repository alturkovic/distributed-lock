for _, key in pairs(KEYS) do
    if redis.call("GET", key) ~= ARGV[1] then
        return false
    end
end

redis.call("DEL", unpack(KEYS))

return true

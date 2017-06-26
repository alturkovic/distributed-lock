local released = 0
for _, key in pairs(KEYS) do
    if redis.call("GET", key) == ARGV[1] then
        released = released + redis.call("DEL", key)
    end
end

return released

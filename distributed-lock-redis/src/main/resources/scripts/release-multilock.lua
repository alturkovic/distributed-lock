local released = 0
for _, key in pairs(KEYS) do
    if redis.call("get", key) == ARGV[1] then
        released = released + redis.call("del", key)
    end
end

return released

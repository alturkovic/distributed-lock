local msetnx_keys_with_tokens = {}

for _, key in ipairs(KEYS) do
    msetnx_keys_with_tokens[#msetnx_keys_with_tokens + 1] = key
    msetnx_keys_with_tokens[#msetnx_keys_with_tokens + 1] = ARGV[1]
end

local keys_successfully_set = redis.call('MSETNX', unpack(msetnx_keys_with_tokens))

if (keys_successfully_set == 0) then
    return false
end

local expiration = tonumber(ARGV[2])
for _, key in ipairs(KEYS) do
    redis.call('PEXPIRE', key, expiration)
end
return true

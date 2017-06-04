-- Copyright (c)  2017 Alen Turković <alturkovic@gmail.com>
--
-- Permission to use, copy, modify, and distribute this software for any
-- purpose with or without fee is hereby granted, provided that the above
-- copyright notice and this permission notice appear in all copies.
--
-- THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
-- WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
-- MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
-- ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
-- WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
-- ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
-- OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

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
    redis.call('EXPIRE', key, expiration)
end
return true

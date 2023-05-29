import requests
"""
url = 'https://github.com/settings/apps/rest-key'
token = "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"
headers = {
    'Accept': 'application/vnd.github+json',
    'Authorization': 'Bearer %s' % token,
    'X-GitHub-Api-Version': '2022-11-28'
}
response = requests.post(url)#, headers=headers)
print(response)
"""

# Refresca el token
"""
url = 'https://github.com/login/oauth/access_token'
headers = {
    'Accept': 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28'
}
body = {
    "client_id": "321135",
    "client_secret": "0995bd0579d08fe38527969775f647ff9e5fe368",
    "grant_type": "refresh_token",
    "refresh_token": "",
}
response = requests.post(url, headers=headers, json=body)
res = response.json()
print(res)


curl --request POST \
--url "https://api.github.com/authorizations" \
--user "Iv1.626d7c170e9c67b9:0995bd0579d08fe38527969775f647ff9e5fe368" \
--header "X-GitHub-Api-Version: 2022-11-28"
"""

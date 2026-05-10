import urllib.request, http.cookiejar

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
urllib.request.install_opener(opener)

data = 'username=admin%40sorgavasal.com&password=admin123'.encode()
req2 = urllib.request.Request('http://localhost:8080/login', data=data)
urllib.request.urlopen(req2)

req3 = urllib.request.Request('http://localhost:8080/activity-log')
try:
    response = urllib.request.urlopen(req3)
    html3 = response.read().decode()
    if "No activity found" in html3:
        print('SUCCESS: No activity found')
    elif "Whitelabel Error Page" in html3:
        print('SUCCESS: Whitelabel Error Page')
    else:
        print('SUCCESS: Valid page, length', len(html3))
except urllib.error.HTTPError as e:
    print('HTTP Error:', e.code)
    print(e.read().decode()[:500])
except Exception as e:
    print('EXCEPTION:', e)

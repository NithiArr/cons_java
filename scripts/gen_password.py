import hashlib, base64, os

def make_django_pbkdf2(password, iterations=260000):
    chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    salt = ''.join(chars[b % len(chars)] for b in os.urandom(12))
    dk = hashlib.pbkdf2_hmac('sha256', password.encode(), salt.encode(), iterations, dklen=32)
    hash_b64 = base64.b64encode(dk).decode()
    return 'pbkdf2_sha256$' + str(iterations) + '$' + salt + '$' + hash_b64

print(make_django_pbkdf2('agilan@123'))

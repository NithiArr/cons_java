"""
Quick test to check if the GET /api/expenses/{id} endpoint works
"""
import requests

# Test the endpoint
try:
    # Try to get expense ID 1
    response = requests.get('http://localhost:5000/api/expenses/1')
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.text}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"\nExpense ID: {data.get('expense_id')}")
        print(f"Category: {data.get('category')}")
        print(f"Items: {data.get('items', 'No items')}")
    elif response.status_code == 404:
        print("Expense not found - check if data exists in DB")
    elif response.status_code == 401:
        print("Unauthorized - need to be logged in")
except Exception as e:
    print(f"Error: {e}")

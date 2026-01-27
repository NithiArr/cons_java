# Construction Management System - Quick Reference

## 🚀 How to Start the Application

```bash
cd e:\Personal\Construction
python app.py
```

Then open: **http://127.0.0.1:5000**

---

## 👥 Login Credentials (All Users)

### OWNER (Read-only, all dashboards)
- Email: `nithiarrvind@gmail.com`
- Password: [Your original password]
- User ID: 1

### ADMIN (Full access to everything)
- Email: `admin@sorgavasal.com`
- Password: `admin123`
- User ID: 2

### MANAGER (Projects, Vendors, Purchases)
- Email: `manager@tsconst.com`
- Password: `manager123`
- User ID: 3

### ACCOUNTANT (Expenses, Payments)
- Email: `accountant@tsconst.com`
- Password: `accountant123`
- User ID: 4

### ENGINEER (View-only)
- Email: `engineer@tsconst.com`
- Password: `engineer123`
- User ID: 5

---

## 📊 Sample Data Included

- **5 Projects**: Skyline Tower (₹1.5Cr), Green Valley (₹80L), Corporate Office (₹1.2Cr), Luxury Villa (₹50L), Mall Renovation (₹30L)
- **7 Vendors**: Steel, Cement, Bricks, Electrical, Plumbing, Paint, Hardware
- **7 Purchases** with detailed items
- **10 Expenses**: Labor, equipment, transportation
- **6 Vendor Payments**
- **7 Client Payments**

---

## 🛠️ Useful Scripts

### View All Users
```bash
python view_users.py
```

### Create Additional Admin
```bash
python create_admin.py
```

### Re-seed Sample Data (if needed)
```bash
python seed_data.py
```

### Create More Role Users
```bash
python create_role_users.py
```

---

## 📍 Available Pages

- `/` - Home (redirects based on role)
- `/login` - Login page
- `/register` - Register new users
- `/dashboard` - Main dashboard (all roles except OWNER)
- `/dashboard/owner` - Owner dashboard (OWNER only)
- `/projects` - Manage projects
- `/vendors` - Manage vendors
- `/purchases` - Record purchases
- `/expenses` - Track expenses
- `/payments` - Vendor payments
- `/client-payments` - Client payments
- `/vendor-analytics` - Vendor analytics dashboard
- `/daily-cash` - Daily cash balance tracker

---

## 📂 Database Location

**SQLite Database**: `e:\Personal\Construction\instance\construction.db`

You can view it with:
- DB Browser for SQLite (download from sqlitebrowser.org)
- Python: `python view_users.py`

---

## 🎯 Key Features

1. **Multi-company isolation** - Each company's data is separate
2. **5-tier role-based access** - Different permissions for each role
3. **Owner Dashboard** - High-level KPIs and financial overview
4. **Vendor Analytics** - Purchase history and material summaries
5. **Daily Cash Balance** - Track project cash flow
6. **Export functionality** - PDF and Excel reports
7. **Modern UI** - Dark theme with glassmorphism

---

## 🔧 Tech Stack

- **Backend**: Python Flask, SQLAlchemy
- **Frontend**: HTML, CSS, Vanilla JavaScript
- **Database**: SQLite (can upgrade to PostgreSQL/MySQL)
- **Charts**: Chart.js
- **Export**: ReportLab (PDF), OpenPyXL (Excel)

---

## 📝 Important Files

- `app.py` - Main Flask application
- `models.py` - Database models
- `auth.py` - Authentication
- `api.py` - REST API endpoints
- `dashboard_api.py` - Dashboard calculations
- `pages.py` - HTML page routes
- `export.py` - PDF/Excel export
- `static/css/style.css` - Design system
- `static/js/main.js` - Shared JavaScript

---

## 🐛 Troubleshooting

### Pages showing "Not Found"
```bash
# Force restart the server
taskkill /F /IM python.exe
python app.py
```

### Database issues
- Database is in `instance/construction.db`
- Use `python view_users.py` to check users
- Re-run `python seed_data.py` to reset sample data

### Can't login
- Use `python view_users.py` to see all user emails
- Default admin: `admin@sorgavasal.com / admin123`

---

## 📚 Documentation

- Full implementation details: `implementation_plan.md`
- Development walkthrough: `walkthrough.md`
- Task checklist: `task.md`
- Setup instructions: `README.md`

---

**Company**: TS Constructions
**Location**: e:\Personal\Construction
**Server**: http://127.0.0.1:5000

**All files are saved and ready to use!** 🎉

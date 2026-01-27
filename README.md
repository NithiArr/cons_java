# 🏗️ Construction Management System

A comprehensive web-based construction management system built with Python Flask backend and modern HTML/CSS/JS frontend. The system supports multi-company operations with strict data isolation, 5-tier role-based access control, and comprehensive financial tracking.

## ✨ Features

### Multi-Company Support
- Complete data isolation between companies
- Each user belongs to one company
- Secure authentication and session management

### 5-Tier Role-Based Access Control
- **OWNER**: Read-only access to all data with high-level KPIs and analytics
- **ADMIN**: Full CRUD access to all features, can manage users
- **MANAGER**: Can manage projects and purchases
- **ACCOUNTANT**: Can manage payments, expenses, and financial records
- **ENGINEER**: View-only access to project data

### Core Functionality
- **Projects Management**: Track projects with status, budget, and location
- **Vendors Management**: Maintain vendor database with GST numbers
- **Purchases**: Multi-item purchase orders linked to projects and vendors
- **Expenses**: Track project expenses with bill uploads and payment modes
- **Vendor Payments**: Record payments to vendors for purchases
- **Client Payments**: Track incoming payments from clients

### Specialized Dashboards
1. **Owner Dashboard**
   - High-level KPIs (total projects, budget, spent, outstanding)
   - Status-wise project overview with charts
   - Drill-down project financial table with color indicators
   
2. **Vendor Analytics**
   - Vendor summary (total purchases, paid, outstanding)
   - Purchase history drill-down
   - Material-wise summary from purchase items
   
3. **Daily Cash Balance**
   - Per-project daily cash tracking
   - Opening/closing balance calculation
   - Transaction breakdown
   - PDF/Excel export support

## 🚀 Getting Started

### Prerequisites
- Python 3.8 or higher
- pip (Python package installer)

### Installation

1. **Clone or navigate to the project directory**
```bash
cd e:\Personal\Construction
```

2. **Install dependencies**
```bash
pip install -r requirements.txt
```

3. **Run the application**
```bash
python app.py
```

4. **Access the application**
Open your browser and navigate to:
```
http://127.0.0.1:5000
```

## 📊 Tech Stack

### Backend
- **Flask**: Web framework
- **SQLAlchemy**: ORM for database operations
- **Flask-Login**: User session management
- **SQLite**: Database (production: MySQL/PostgreSQL)
- **ReportLab**: PDF generation
- **OpenPyXL**: Excel export

### Frontend
- **HTML5**: Semantic markup
- **Vanilla CSS**: Modern design with glassmorphism
- **Vanilla JavaScript**: Dynamic interactions
- **Chart.js**: Data visualization

## 🗄️ Database Schema

The system uses 9 interconnected tables:
1. **Company**: Multi-tenant isolation
2. **User**: Authentication with 5 roles
3. **Project**: Project management with status
4. **Vendor**: Company-specific vendors
5. **Expense**: Project expenses
6. **Purchase**: Purchase orders
7. **PurchaseItem**: Line items for purchases
8. **Payment**: Vendor payments
9. **ClientPayment**: Incoming client payments

## 🔐 Security Features

- Password hashing with Werkzeug
- Session-based authentication
- Company-level data isolation
- Role-based API endpoint protection
- SQL injection prevention via SQLAlchemy ORM

## 📱 User Interface

- **Dark Mode**: Professional dark theme with vibrant accents
- **Glassmorphism**: Modern frosted glass card effects
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Smooth Animations**: Micro-interactions for better UX
- **Color-coded Indicators**: Visual budget status (🔴 over, 🟡 near, 🟢 healthy)

## 🎯 Key Business Rules

- All data is company-scoped
- Projects have status tracking (PLANNING, ACTIVE, ON_HOLD, COMPLETED)
- Purchases can have multiple items
- Payments link to specific purchases or can be standalone
- Client payments increase project cash balance
- Expenses and purchases reduce project cash balance
- Outstanding = Spent - Received
- Balance Budget = Budget - Spent

## 📈 KPI Calculations

### Owner Dashboard
- Total Budget: Sum of all project budgets
- Total Spent: Sum of all expenses and purchases
- Total Received: Sum of all client payments
- Outstanding: Total Spent - Total Received

### Daily Cash Balance
- Opening Balance: Previous day's closing balance
- Closing Balance: Opening + Client Receipts - Expenses - Vendor Payments

## 🔧 Configuration

### Environment Variables
- `SECRET_KEY`: Flask secret key (default: 'dev-secret-key-change-in-production')
- Database URI and other configs can be modified in `app.py`

### Production Deployment
1. Set a secure `SECRET_KEY` environment variable
2. Change database from SQLite to PostgreSQL/MySQL
3. Enable HTTPS
4. Configure proper file upload limits
5. Set up CORS if needed for API access

## 📝 First Time Setup

1. **Run the application** - Database tables will be created automatically
2. **Register first user** - Choose "Create New Company"
3. **First user becomes OWNER** - Automatic role assignment
4. **Add more users** - They can join the existing company with other roles

## 🛠️ Development

### File Structure
```
Construction/
├── app.py                  # Main Flask application
├── models.py              # Database models
├── auth.py                # Authentication routes
├── api.py                 # REST API endpoints
├── dashboard_api.py       # Dashboard KPI calculations
├── export.py              # PDF/Excel export
├── requirements.txt       # Python dependencies
├── static/
│   ├── css/
│   │   └── style.css     # Design system
│   └── js/
│       └── main.js       # Shared utilities
└── templates/
    ├── base.html          # Base layout
    ├── login.html         # Authentication
    ├── dashboard.html     # Main dashboard
    ├── owner_dashboard.html  # Owner KPIs
    ├── vendor_analytics.html # Vendor analysis
    └── daily_cash.html    # Cash balance
```

## 📄 License

This project is proprietary software for construction management.

## 👥 Support

For issues or questions, contact your system administrator.

---

**Built with ❤️ for Construction Companies**

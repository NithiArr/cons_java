
import os
from mongoengine import connect
from app import create_app, db
from models_mongo import (
    Company as MongoCompany, User as MongoUser, Project as MongoProject,
    Vendor as MongoVendor, Expense as MongoExpense, Payment as MongoPayment,
    ClientPayment as MongoClientPayment, MasterCategory as MongoMasterCategory
)
from models import (
    Company, User, Project, Vendor, Expense, ExpenseItem, Payment, ClientPayment,
    MasterCategory, SubCategory
)
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Setup Flask app and context
app = create_app()

# Override SQLALCHEMY_DATABASE_URI for migration if needed, otherwise uses what's in .env or app.py
# For now, we assume app.py will be updated to use the Postgres URI, or we set it here.
# Let's set it explicitly here to be safe and ensure we target the right DB.
# Update this with your actual PostgreSQL credentials
# 'postgresql://username:password@localhost:5432/construction_db'
DB_URI = os.environ.get('SQLALCHEMY_DATABASE_URI', 'postgresql://postgres:password@localhost:5432/construction_db')
app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI

# Connect to MongoDB
MONGODB_HOST = os.environ.get('MONGODB_HOST', 'mongodb://localhost:27017/construction_db')
connect(host=MONGODB_HOST)

def migrate():
    with app.app_context():
        print(f"Connecting to PostgreSQL at {DB_URI}...")
        try:
            db.create_all()
            print("PostgreSQL tables created.")
        except Exception as e:
            print(f"Error creating tables: {e}")
            return

        # ID Mapping: ObjectId string -> Integer ID
        id_map = {
            'companies': {},
            'users': {},
            'projects': {},
            'vendors': {},
            'expenses': {},
            'master_categories': {},
        }

        print("Migrating Companies...")
        for m_company in MongoCompany.objects:
            company = Company(
                name=m_company.name,
                address=m_company.address,
                phone=m_company.phone,
                email=m_company.email,
                created_at=m_company.created_at
            )
            db.session.add(company)
            db.session.flush() # Generate ID
            id_map['companies'][str(m_company.id)] = company.company_id
        db.session.commit()
        print(f"Migrated {len(id_map['companies'])} companies.")

        print("Migrating Users...")
        for m_user in MongoUser.objects:
            if str(m_user.company.id) not in id_map['companies']:
                print(f"Skipping user {m_user.email} (Company not found)")
                continue
            
            user = User(
                company_id=id_map['companies'][str(m_user.company.id)],
                name=m_user.name,
                email=m_user.email,
                password_hash=m_user.password_hash,
                role=m_user.role,
                created_at=m_user.created_at
            )
            db.session.add(user)
            db.session.flush()
            id_map['users'][str(m_user.id)] = user.user_id
        db.session.commit()
        print(f"Migrated {len(id_map['users'])} users.")
        
        print("Migrating Master Categories...")
        for m_cat in MongoMasterCategory.objects:
            cat = MasterCategory(
                name=m_cat.name,
                type=m_cat.type,
                is_active=m_cat.is_active
            )
            db.session.add(cat)
            db.session.flush()
            id_map['master_categories'][str(m_cat.id)] = cat.category_id
            
            # Migrate Subcategories
            for m_sub in m_cat.subcategories:
                sub = SubCategory(
                    parent_category_id=cat.category_id,
                    name=m_sub.name,
                    default_unit=m_sub.default_unit
                )
                db.session.add(sub)
        db.session.commit()
        print(f"Migrated {len(id_map['master_categories'])} master categories.")

        print("Migrating Projects...")
        for m_project in MongoProject.objects:
            if str(m_project.company.id) not in id_map['companies']:
                continue

            project = Project(
                company_id=id_map['companies'][str(m_project.company.id)],
                name=m_project.name,
                location=m_project.location,
                start_date=m_project.start_date,
                end_date=m_project.end_date,
                budget=m_project.budget,
                status=m_project.status,
                created_at=m_project.created_at
            )
            db.session.add(project)
            db.session.flush()
            id_map['projects'][str(m_project.id)] = project.project_id
        db.session.commit()
        print(f"Migrated {len(id_map['projects'])} projects.")

        print("Migrating Vendors...")
        for m_vendor in MongoVendor.objects:
            if str(m_vendor.company.id) not in id_map['companies']:
                continue

            vendor = Vendor(
                company_id=id_map['companies'][str(m_vendor.company.id)],
                name=m_vendor.name,
                phone=m_vendor.phone,
                email=m_vendor.email,
                gst_number=m_vendor.gst_number,
                created_at=m_vendor.created_at
            )
            db.session.add(vendor)
            db.session.flush()
            id_map['vendors'][str(m_vendor.id)] = vendor.vendor_id
        db.session.commit()
        print(f"Migrated {len(id_map['vendors'])} vendors.")

        print("Migrating Expenses...")
        for m_expense in MongoExpense.objects:
            comp_id = id_map['companies'].get(str(m_expense.company.id))
            proj_id = id_map['projects'].get(str(m_expense.project.id))
            vend_id = id_map['vendors'].get(str(m_expense.vendor.id)) if m_expense.vendor else None

            if not comp_id or not proj_id:
                print(f"Skipping expense {m_expense.id} (Missing relations)")
                continue

            expense = Expense(
                company_id=comp_id,
                project_id=proj_id,
                vendor_id=vend_id,
                expense_type=m_expense.expense_type,
                category=m_expense.category,
                amount=m_expense.amount,
                payment_mode=m_expense.payment_mode,
                expense_date=m_expense.expense_date,
                description=m_expense.description,
                invoice_number=m_expense.invoice_number,
                bill_url=m_expense.bill_url,
                created_at=m_expense.created_at
            )
            db.session.add(expense)
            db.session.flush()
            id_map['expenses'][str(m_expense.id)] = expense.expense_id

            # Migrate Expense Items
            for m_item in m_expense.items:
                item = ExpenseItem(
                    expense_id=expense.expense_id,
                    item_name=m_item.item_name,
                    quantity=m_item.quantity,
                    unit_price=m_item.unit_price,
                    total_price=m_item.total_price
                )
                db.session.add(item)
        db.session.commit()
        print(f"Migrated {len(id_map['expenses'])} expenses.")

        print("Migrating Payments...")
        for m_payment in MongoPayment.objects:
            comp_id = id_map['companies'].get(str(m_payment.company.id))
            proj_id = id_map['projects'].get(str(m_payment.project.id))
            vend_id = id_map['vendors'].get(str(m_payment.vendor.id))
            # Handle optional expense link
            exp_id = id_map['expenses'].get(str(m_payment.expense.id)) if m_payment.expense else None

            if not all([comp_id, proj_id, vend_id]):
                 print(f"Skipping payment {m_payment.id} (Missing relations)")
                 continue

            payment = Payment(
                company_id=comp_id,
                project_id=proj_id,
                vendor_id=vend_id,
                expense_id=exp_id,
                amount=m_payment.amount,
                payment_date=m_payment.payment_date,
                payment_mode=m_payment.payment_mode,
                created_at=m_payment.created_at
            )
            db.session.add(payment)
        db.session.commit()
        print("Migrated payments.")

        print("Migrating Client Payments...")
        for m_cpayment in MongoClientPayment.objects:
            comp_id = id_map['companies'].get(str(m_cpayment.company.id))
            proj_id = id_map['projects'].get(str(m_cpayment.project.id))

            if not comp_id or not proj_id:
                continue

            cpayment = ClientPayment(
                company_id=comp_id,
                project_id=proj_id,
                amount=m_cpayment.amount,
                payment_date=m_cpayment.payment_date,
                payment_mode=m_cpayment.payment_mode,
                reference_number=m_cpayment.reference_number,
                remarks=m_cpayment.remarks,
                created_at=m_cpayment.created_at
            )
            db.session.add(cpayment)
        db.session.commit()
        print("Migrated client payments.")

        print("Migration Completed Successfully!")

if __name__ == "__main__":
    migrate()

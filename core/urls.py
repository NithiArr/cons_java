from django.urls import path
from . import views, api, export_utils

urlpatterns = [
    path('dashboard/owner/', views.owner_dashboard, name='owner_dashboard'),
    path('dashboard/main/', views.main_dashboard, name='main_dashboard'),
    path('dashboard/daily-cash/', views.daily_cash, name='daily_cash'),
    path('dashboard/vendor-analytics/', views.vendor_analytics, name='vendor_analytics'),
    
    # APIs
    path('dashboard/api/owner-kpis', api.owner_kpis, name='api_owner_kpis'),
    path('dashboard/api/project-financial-table', api.project_financial_table, name='api_project_financial_table'),
    path('dashboard/api/daily-cash-balance', api.daily_cash_balance, name='api_daily_cash_balance'),
    
    # Master Categories
    path('master-categories/', views.master_categories, name='master_categories'),
    path('api/master/categories/', api.master_categories_list, name='api_master_categories_list'),
    path('api/master/categories/<int:category_id>/', api.master_categories_detail, name='api_master_categories_detail'),
    
    # Projects
    path('projects', views.projects, name='projects'),
    path('api/projects', api.projects_list, name='api_projects_list'),
    path('api/projects/<str:project_id>', api.project_detail, name='api_project_detail'), # Using str for UUID
    path('dashboard/api/project-payment-details/<str:project_id>', api.get_project_payment_details, name='api_project_payment_details'),
    path('api/projects/<str:project_id>/export', export_utils.export_project_to_excel, name='export_project_excel'),
    
    # Vendors
    path('vendors', views.vendors, name='vendors'),
    path('dashboard/vendor-analytics', views.vendor_analytics, name='vendor_analytics'),
    path('api/vendors', api.vendors_list, name='api_vendors_list'),
    path('api/vendors/<str:vendor_id>', api.vendor_detail, name='api_vendor_detail'),
    path('dashboard/api/vendor-material-summary/<str:vendor_id>', api.get_vendor_material_summary, name='api_vendor_material_summary'),
    path('dashboard/api/vendor-summary', api.get_vendor_summary, name='api_vendor_summary'),
    path('dashboard/api/vendor-purchase-history/<str:vendor_id>', api.get_vendor_purchase_history, name='api_vendor_purchase_history'),
    path('api/vendors/<str:vendor_id>/outstanding', api.get_vendor_outstanding, name='api_vendor_outstanding'),
]

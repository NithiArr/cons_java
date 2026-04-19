--
-- PostgreSQL database dump
--

-- Dumped from database version 16.1
-- Dumped by pg_dump version 16.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.company (company_id, name, address, phone, email, created_at) FROM stdin;
1	Sorgavasal	123 Construction St, Chennai	9876543210	info@sorgavasal.com	2026-02-16 14:49:01.018473+05:30
2	TS	21 B, Type II Quaters,	09442052861	ts@gmail.com	2026-02-16 14:49:01.018473+05:30
\.


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user (password, last_login, is_superuser, first_name, last_name, is_staff, is_active, date_joined, user_id, email, name, role, company_id) FROM stdin;
pbkdf2_sha256$1000000$yfpSMavTIAm0d52Mg10aBH$nkdgKncOg9cwXk/vdoc2KE6DwEysJT97IYB15yPPNl0=	2026-03-26 23:12:31.651464+05:30	f			f	t	2026-03-26 22:25:24.461275+05:30	6	agilan@gmail.com	Agilan	MANAGER	1
pbkdf2_sha256$1000000$bonPF0hzljBHC46MvahTWC$DpLw/kbY7cUPJlPrx3cXBvYqL2IrlJN/hZOGxQ9oEqk=	2026-03-26 23:13:02.823797+05:30	f			f	t	2026-03-26 22:26:49.189359+05:30	7	gokul@gmail.com	Gokul	EMPLOYEE	1
pbkdf2_sha256$1000000$hmKKCuuXHiM1H46eDTx73U$AhdFDpDO/SixH7hgooEqWLl5kEBufgQraapHaV5eQz4=	2026-03-26 23:28:09.251837+05:30	f			t	t	2026-01-31 22:15:22+05:30	3	admin@sorgavasal.com	Admin User	ADMIN	1
\.


--
-- Data for Name: auth_group; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_group (id, name) FROM stdin;
\.


--
-- Data for Name: app_user_groups; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user_groups (id, user_id, group_id) FROM stdin;
\.


--
-- Data for Name: django_content_type; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_content_type (id, app_label, model) FROM stdin;
1	admin	logentry
2	auth	permission
3	auth	group
4	contenttypes	contenttype
5	sessions	session
6	users	company
7	users	user
8	core	mastercategory
9	core	project
10	core	subcategory
11	core	vendor
12	finance	clientpayment
13	finance	expense
14	finance	expenseitem
15	finance	payment
\.


--
-- Data for Name: auth_permission; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_permission (id, name, content_type_id, codename) FROM stdin;
1	Can add log entry	1	add_logentry
2	Can change log entry	1	change_logentry
3	Can delete log entry	1	delete_logentry
4	Can view log entry	1	view_logentry
5	Can add permission	2	add_permission
6	Can change permission	2	change_permission
7	Can delete permission	2	delete_permission
8	Can view permission	2	view_permission
9	Can add group	3	add_group
10	Can change group	3	change_group
11	Can delete group	3	delete_group
12	Can view group	3	view_group
13	Can add content type	4	add_contenttype
14	Can change content type	4	change_contenttype
15	Can delete content type	4	delete_contenttype
16	Can view content type	4	view_contenttype
17	Can add session	5	add_session
18	Can change session	5	change_session
19	Can delete session	5	delete_session
20	Can view session	5	view_session
21	Can add company	6	add_company
22	Can change company	6	change_company
23	Can delete company	6	delete_company
24	Can view company	6	view_company
25	Can add user	7	add_user
26	Can change user	7	change_user
27	Can delete user	7	delete_user
28	Can view user	7	view_user
29	Can add master category	8	add_mastercategory
30	Can change master category	8	change_mastercategory
31	Can delete master category	8	delete_mastercategory
32	Can view master category	8	view_mastercategory
33	Can add project	9	add_project
34	Can change project	9	change_project
35	Can delete project	9	delete_project
36	Can view project	9	view_project
37	Can add sub category	10	add_subcategory
38	Can change sub category	10	change_subcategory
39	Can delete sub category	10	delete_subcategory
40	Can view sub category	10	view_subcategory
41	Can add vendor	11	add_vendor
42	Can change vendor	11	change_vendor
43	Can delete vendor	11	delete_vendor
44	Can view vendor	11	view_vendor
45	Can add client payment	12	add_clientpayment
46	Can change client payment	12	change_clientpayment
47	Can delete client payment	12	delete_clientpayment
48	Can view client payment	12	view_clientpayment
49	Can add expense	13	add_expense
50	Can change expense	13	change_expense
51	Can delete expense	13	delete_expense
52	Can view expense	13	view_expense
53	Can add expense item	14	add_expenseitem
54	Can change expense item	14	change_expenseitem
55	Can delete expense item	14	delete_expenseitem
56	Can view expense item	14	view_expenseitem
57	Can add payment	15	add_payment
58	Can change payment	15	change_payment
59	Can delete payment	15	delete_payment
60	Can view payment	15	view_payment
\.


--
-- Data for Name: app_user_user_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user_user_permissions (id, user_id, permission_id) FROM stdin;
\.


--
-- Data for Name: auth_group_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_group_permissions (id, group_id, permission_id) FROM stdin;
\.


--
-- Data for Name: project; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.project (project_id, name, location, start_date, end_date, budget, status, created_at, company_id) FROM stdin;
1	Green Valley Apartments	Powai, Mumbai	2025-02-15	2026-03-15	8000000.00	ACTIVE	2026-02-16 14:49:01.06285+05:30	1
2	Old City Mall Renovation	Dadar	2024-08-01	2024-12-31	3000000.00	COMPLETED	2026-02-16 14:49:01.06365+05:30	1
3	Luxury Villa Project	Lonavala	2025-03-01	2025-09-30	5000000.00	PLANNING	2026-02-16 14:49:01.06365+05:30	1
4	Corporate Office Complex	Andheri East, Mumbai	2024-11-01	2025-10-30	12000000.00	ACTIVE	2026-02-16 14:49:01.06365+05:30	1
5	CIT Hostel	CIT	2026-02-02	2026-04-30	150000000.00	ACTIVE	2026-02-16 14:49:01.06365+05:30	1
6	NewTest	Blr	2026-02-21	2026-02-21	10.00	COMPLETED	2026-02-21 23:28:39.048365+05:30	1
7	DSP SIte	Podanur	\N	\N	4580000.00	ACTIVE	2026-02-23 16:01:49.266088+05:30	1
8	Nithish House	Chennai	2026-03-01	2026-03-31	50000000.00	ACTIVE	2026-02-25 22:43:16.391907+05:30	1
9	Echanari Project	Coimbatore	2026-01-31	2026-05-31	3500000.00	ACTIVE	2026-02-27 13:29:55.702161+05:30	1
\.


--
-- Data for Name: client_payment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_payment (client_payment_id, amount, payment_date, payment_mode, reference_number, remarks, created_at, company_id, project_id) FROM stdin;
1	4000000.00	2025-01-05	BANK	CHQ-002345	Advance payment	2026-02-16 14:49:01.097239+05:30	1	4
2	3000000.00	2024-12-30	BANK	CHQ-000123	Final payment on completion	2026-02-16 14:49:01.097239+05:30	1	2
3	3500000.00	2025-02-25	BANK	CHQ-002678	Progress payment	2026-02-16 14:49:01.097239+05:30	1	4
4	2500000.00	2026-02-02	BANK	\N	\N	2026-02-16 14:49:01.097239+05:30	1	5
5	2500000.00	2025-02-25	BANK	NEFT-789012	Booking amount	2026-02-16 14:49:01.103043+05:30	1	1
6	1500000.00	2025-03-20	BANK	NEFT-789345	Construction start payment	2026-02-16 14:49:01.103329+05:30	1	1
7	250000.00	2026-02-25	CASH	\N	Initial Payment	2026-02-25 23:21:05.690949+05:30	1	8
8	100000.00	2026-02-25	CASH	\N	payment	2026-02-25 23:50:56.427444+05:30	1	8
9	100000.00	2026-01-26	CASH	\N	Advance	2026-02-27 13:30:41.09613+05:30	1	9
10	350000.00	2026-02-02	CASH	\N	Advance	2026-02-27 13:31:21.73567+05:30	1	9
11	14000.00	2026-02-21	UPI	\N	Extra Work Bill 1	2026-02-27 13:33:08.29725+05:30	1	9
12	5000.00	2026-04-05	CASH	\N	Test payment	2026-04-05 21:00:46.574505+05:30	1	8
\.


--
-- Data for Name: django_admin_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_admin_log (id, action_time, object_id, object_repr, action_flag, change_message, content_type_id, user_id) FROM stdin;
\.


--
-- Data for Name: django_migrations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_migrations (id, app, name, applied) FROM stdin;
1	contenttypes	0001_initial	2026-02-16 14:47:27.421663+05:30
2	contenttypes	0002_remove_content_type_name	2026-02-16 14:47:27.43139+05:30
3	auth	0001_initial	2026-02-16 14:47:27.489217+05:30
4	auth	0002_alter_permission_name_max_length	2026-02-16 14:47:27.494222+05:30
5	auth	0003_alter_user_email_max_length	2026-02-16 14:47:27.499528+05:30
6	auth	0004_alter_user_username_opts	2026-02-16 14:47:27.504772+05:30
7	auth	0005_alter_user_last_login_null	2026-02-16 14:47:27.509964+05:30
8	auth	0006_require_contenttypes_0002	2026-02-16 14:47:27.51183+05:30
9	auth	0007_alter_validators_add_error_messages	2026-02-16 14:47:27.51551+05:30
10	auth	0008_alter_user_username_max_length	2026-02-16 14:47:27.520663+05:30
11	auth	0009_alter_user_last_name_max_length	2026-02-16 14:47:27.525489+05:30
12	auth	0010_alter_group_name_max_length	2026-02-16 14:47:27.532824+05:30
13	auth	0011_update_proxy_permissions	2026-02-16 14:47:27.537272+05:30
14	auth	0012_alter_user_first_name_max_length	2026-02-16 14:47:27.543439+05:30
15	users	0001_initial	2026-02-16 14:47:27.610807+05:30
16	admin	0001_initial	2026-02-16 14:47:27.640327+05:30
17	admin	0002_logentry_remove_auto_add	2026-02-16 14:47:27.647315+05:30
18	admin	0003_logentry_add_action_flag_choices	2026-02-16 14:47:27.656183+05:30
19	core	0001_initial	2026-02-16 14:47:27.690275+05:30
20	core	0002_initial	2026-02-16 14:47:27.719217+05:30
21	finance	0001_initial	2026-02-16 14:47:27.749163+05:30
22	finance	0002_initial	2026-02-16 14:47:27.880776+05:30
23	sessions	0001_initial	2026-02-16 14:47:27.892991+05:30
24	users	0002_alter_user_password	2026-02-16 14:48:59.60715+05:30
25	users	0003_alter_user_table	2026-02-17 12:42:36.301395+05:30
26	finance	0003_expenseitem_brand	2026-02-21 19:20:00.601694+05:30
27	users	0004_alter_user_role	2026-03-26 22:14:44.417914+05:30
\.


--
-- Data for Name: django_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_session (session_key, session_data, expire_date) FROM stdin;
4ukqytsgr69tv46elntshlefkkvf7zu3	.eJxVjMsOwiAQRf-FtSHQmfJw6d5vIAwDUjU0Ke3K-O_apAvd3nPOfYkQt7WGreclTCzOAsTpd6OYHrntgO-x3WaZ5rYuE8ldkQft8jpzfl4O9--gxl6_tfGOY0FjB80A1pKxWmXkkSB5igOqQmgLK4faj5i0RV-8I-TCCRDE-wPRezeF:1vrvjW:8TDVX9D-UNisK-_QFl9hZjDU08rxfitlSW8-DKL7H44	2026-03-02 15:51:38.130137+05:30
wmgi1pwyxn6hlf2mcuh2bzpt59hvv6gp	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vtrDP:leG11Sh5O5if-ftEvuHvxd4FdqetxJjha4BZaoU0M08	2026-03-07 23:26:27.957674+05:30
d9cfawr8kbcb7r6you6lwdlmdrbwpsxa	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vtrEa:MPkxF2BzFtTbWvB9BFxUecg-OO_kS5J-61R4z-xbfTM	2026-03-07 23:27:40.218721+05:30
1fjad7w022fxbu2ymtcytxcfg2i0w0ns	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vuT90:apTU6ku3gHYjmCdYfusi81z1nCOryWIqIqQtcghgvyA	2026-03-09 15:56:26.246004+05:30
fl6tvhdqls41f06rc9tqdnyioyit2yss	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vuu4G:fpZtpsIAUEqk4nZAEFyflmlTKOJ3tLVglvoGEkR-4Ws	2026-03-10 20:41:20.343729+05:30
lga3ruzlj27y9zklfjd2mqs9twksnni9	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vyNal:5L6Yh_5zz7309lStYyqDFCraiuNUewvsyGkGHqR9niU	2026-03-20 10:49:15.618489+05:30
am42fpg3utug6en067gxqq8vyupr46po	.eJxVjEEOwiAQRe_C2hCmAy24dO8ZyDCAVA0kpV0Z765NutDtf-_9l_C0rcVvPS1-juIsUJx-t0D8SHUH8U711iS3ui5zkLsiD9rltcX0vBzu30GhXr51Hq3OrIgtTjm4oBI4m8gqttphVJCdRiACGA1YQ6CTHgZE1gYiTyDeH-JxNzc:1vyNxH:VQ3-qgU1gyvSx1P2YbIbqGFL-8VA5E8a3eq84qlf59k	2026-03-20 11:12:31.395472+05:30
h9epk682q9574h2yd2lm58e5u6vyytni	.eJxVjMsOwiAQRf-FtSHQmfJw6d5vIAwDUjU0Ke3K-O_apAvd3nPOfYkQt7WGreclTCzOAsTpd6OYHrntgO-x3WaZ5rYuE8ldkQft8jpzfl4O9--gxl6_tfGOY0FjB80A1pKxWmXkkSB5igOqQmgLK4faj5i0RV-8I-TCCRDE-wPRezeF:1w5oy9:87jM-8aEAgvOYKTPZ2L_oXATqYQn1xjjO9CbTg7SZRM	2026-04-09 23:28:09.256944+05:30
\.


--
-- Data for Name: vendor; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vendor (vendor_id, name, phone, email, gst_number, created_at, company_id) FROM stdin;
1	Cement Corporation	9876543211	info@cement.com	27AABCC5678G2Z2	2026-02-16 14:49:01.067202+05:30	1
2	Plumbing Pro	9876543214	info@plumbingpro.com	27AABCP7890J5Z5	2026-02-16 14:49:01.070315+05:30	1
3	Hardware Traders	9876543216	orders@hardware.com	27AABCH3344L7Z7	2026-02-16 14:49:01.070315+05:30	1
4	Steel Suppliers Ltd	9876543210	steel@suppliers.com	27AABCS1234F1Z1	2026-02-16 14:49:01.070315+05:30	1
5	Nithish Sengal Mart	489421845	nithiarrvind@gmail.com	282asdasdq	2026-02-16 14:49:01.071315+05:30	1
6	Brick Masters	9876543212	sales@brickmasters.com	27AABCB9012H3Z3	2026-02-16 14:49:01.071315+05:30	1
7	Electrical Solutions	9876543213	contact@electrical.com	27AABCE3456I4Z4	2026-02-16 14:49:01.071315+05:30	1
8	Paint & Finishes Co	9876543215	sales@paints.com	27AABCF1122K6Z6	2026-02-16 14:49:01.07232+05:30	1
9	AK STEELS	\N	\N	\N	2026-02-27 14:59:12.536494+05:30	1
10	KRISHNA BUILDING MATERIALS	\N	\N	\N	2026-02-27 14:59:33.33905+05:30	1
11	PALANI MURUGAN TRADERS	\N	\N	\N	2026-02-27 14:59:42.631122+05:30	1
12	SV TRADERS	\N	\N	\N	2026-02-27 14:59:54.936659+05:30	1
\.


--
-- Data for Name: expense; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.expense (expense_id, expense_type, category, amount, payment_mode, expense_date, description, invoice_number, bill_url, created_at, company_id, project_id, vendor_id) FROM stdin;
5	Material Purchase	Sengal	10000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	5	5
6	Material Purchase	Sengal	500000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	1	5
7	Material Purchase	Steel Bars	350000.00	CREDIT	2025-02-20	Invoice #INV-2025-010	INV-2025-010	\N	2026-02-16 14:49:01.07432+05:30	1	1	4
9	Material Purchase	Steel Bars	1500000.00	CREDIT	2026-02-02	Invoice #INV-2025-002	INV-2025-002	\N	2026-02-16 14:49:01.07432+05:30	1	5	4
12	Material Purchase	Sengal	20000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	5	5
13	Material Purchase	Electrical Cables	180000.00	CREDIT	2025-03-01	Invoice #INV-2025-011	INV-2025-011	\N	2026-02-16 14:49:01.07432+05:30	1	1	7
14	Material Purchase	Cement	400000.00	CREDIT	2025-01-10	Invoice #INV-2025-020	INV-2025-020	\N	2026-02-16 14:49:01.07432+05:30	1	4	1
15	Material Purchase	Asian Paints	250000.00	CREDIT	2025-02-15	Invoice #INV-2025-021	INV-2025-021	\N	2026-02-16 14:49:01.07432+05:30	1	4	8
16	Material Purchase	Electrical Cables	2500000.00	CREDIT	2026-02-02	Invoice #INV-2025-010	INV-2025-010	\N	2026-02-16 14:49:01.07432+05:30	1	5	7
17	Material Purchase	Asian Paints	1000000.00	CASH	2026-02-02	Invoice #inv 100	inv 100	\N	2026-02-16 14:49:01.07432+05:30	1	5	8
24	Material Purchase	Steel	50000.00	CASH	2026-02-16	Invoice #		\N	2026-02-16 21:17:34.989448+05:30	1	5	7
26	Material Purchase	Steel	125000.00	CASH	2026-02-16	Invoice #		\N	2026-02-16 21:48:12.779012+05:30	1	5	4
1	Regular Expense	Skilled Labor	120000.00	BANK	2025-02-28		\N	\N	2026-02-16 14:49:01.07332+05:30	1	1	\N
2	Regular Expense	Skilled Labor	180000.00	BANK	2025-01-31		\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
3	Regular Expense	Miscellaneous	15000.00	CASH	2025-02-10		\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
4	Regular Expense	Electricity Bill	42000.00	BANK	2025-02-20		\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
8	Regular Expense	Safety Equipment	35000.00	UPI	2025-03-05		\N	\N	2026-02-16 14:49:01.07432+05:30	1	1	\N
10	Regular Expense	Skilled Labor	15000.00	CASH	2026-02-02		\N	\N	2026-02-16 14:49:01.07432+05:30	1	5	\N
11	Regular Expense	Site Supervision	60000.00	BANK	2025-03-01		\N	\N	2026-02-16 14:49:01.07432+05:30	1	1	\N
18	Regular Expense	Petty Cash	2000.00	BANK	2026-02-16		\N	\N	2026-02-16 20:36:36.82613+05:30	1	5	\N
19	Regular Expense	Petty Cash	50000.00	CASH	2026-02-16		\N	\N	2026-02-16 21:02:47.176582+05:30	1	5	\N
20	Regular Expense	Petty Cash	5000.00	CASH	2026-02-15		\N	\N	2026-02-16 21:03:12.734585+05:30	1	5	\N
27	Regular Expense	Transport	20000.00	CASH	2026-02-21	Tata Ace	\N	\N	2026-02-21 18:59:33.204488+05:30	1	5	\N
28	Material Purchase	Steel	200000.00	CASH	2026-02-21	\N		\N	2026-02-21 23:44:41.054084+05:30	1	5	4
29	Material Purchase	Brick	150000.00	CREDIT	2026-02-25	\N		\N	2026-02-25 22:45:32.522657+05:30	1	8	6
30	Regular Expense	Petty Cash	25000.00	CASH	2026-02-25	Pooja items	\N	\N	2026-02-25 22:47:18.85554+05:30	1	8	\N
31	Material Purchase	Steel	55000.00	CASH	2026-02-25	\N		\N	2026-02-25 22:48:17.841332+05:30	1	8	2
32	Material Purchase	Painting	65000.00	CREDIT	2026-02-25	\N		\N	2026-02-25 22:49:49.233063+05:30	1	8	8
33	Material Purchase	Brick	1045.50	CREDIT	2026-02-25	\N		\N	2026-02-25 22:59:28.388204+05:30	1	8	6
34	Regular Expense	Petty Cash	300.00	CASH	2026-01-31	Marking Materials Nylon,White Cement	\N	\N	2026-02-27 13:36:53.481167+05:30	1	9	\N
35	Regular Expense	Transport	200.00	CASH	2026-01-31	Labour Bike Petrol	\N	\N	2026-02-27 13:38:25.326857+05:30	1	9	\N
36	Regular Expense	Material	160.00	CASH	2026-02-03	Cutting blade	\N	\N	2026-02-27 13:43:40.1129+05:30	1	9	\N
38	Regular Expense	TSK Expense	60000.00	BANK	2026-02-04	Tamil Karthi	\N	\N	2026-02-27 14:21:04.720668+05:30	1	9	\N
37	Regular Expense	Material	540.00	UPI	2026-02-04	Mammutty 2 nos	\N	\N	2026-02-27 14:20:22.934316+05:30	1	9	\N
39	Regular Expense	Labour	10000.00	CASH	2026-02-04	Ramesh adv labour	\N	\N	2026-02-27 14:35:46.337737+05:30	1	9	\N
40	Regular Expense	OTHERS	1750.00	BANK	2026-02-04	Water Testing ACC	\N	\N	2026-02-27 14:38:48.081143+05:30	1	9	\N
41	Regular Expense	Petty Cash	100.00	CASH	2026-02-06	Sara Kairu For Green Net	\N	\N	2026-02-27 14:40:20.443485+05:30	1	9	\N
42	Regular Expense	OTHERS	1500.00	CASH	2026-02-06	Green Net 	\N	\N	2026-02-27 14:41:17.587972+05:30	1	9	\N
43	Regular Expense	Transport	90.00	UPI	2026-02-06	Porter 	\N	\N	2026-02-27 14:44:45.757647+05:30	1	9	\N
44	Regular Expense	Labour	8700.00	CASH	2026-02-07	Mason Labour	\N	\N	2026-02-27 14:45:15.880797+05:30	1	9	\N
45	Regular Expense	Labour	8250.00	CASH	2026-02-07	Ramesh Fitter 	\N	\N	2026-02-27 14:58:21.387913+05:30	1	9	\N
46	Material Purchase	Steel	133115.00	CREDIT	2026-02-02	\N	11111	\N	2026-02-27 15:01:46.091831+05:30	1	9	9
47	Material Purchase	Steel	66220.00	CREDIT	2026-02-03	\N		\N	2026-02-27 15:05:32.836495+05:30	1	9	9
48	Material Purchase	Building Materials	28000.00	CREDIT	2026-02-04	\N		\N	2026-02-27 15:09:14.524743+05:30	1	9	10
49	Material Purchase	Building Materials	17400.00	CREDIT	2026-02-04	\N		\N	2026-02-27 15:11:22.494978+05:30	1	9	11
50	Material Purchase	Building Materials	34800.00	CREDIT	2026-02-13	\N		\N	2026-02-27 15:30:23.559276+05:30	1	9	11
51	Material Purchase	Building Materials	38000.00	CREDIT	2026-02-13	\N		\N	2026-02-27 15:31:02.969061+05:30	1	9	10
52	Material Purchase	Building Materials	49500.00	CREDIT	2026-02-13	\N		\N	2026-02-27 15:31:44.993104+05:30	1	9	10
53	Material Purchase	Building Materials	14500.00	CREDIT	2026-02-13	\N		\N	2026-02-27 15:32:42.558109+05:30	1	9	11
54	Material Purchase	Brick	22500.00	CREDIT	2026-02-23	\N		\N	2026-02-27 15:42:14.771702+05:30	1	9	12
55	Material Purchase	Steel	60993.60	CREDIT	2026-02-24	\N		\N	2026-02-27 15:50:54.562607+05:30	1	9	9
56	Regular Expense	OTHERS	15000.00	CASH	2026-02-07	Puttu Maram for green net	\N	\N	2026-02-27 15:59:57.16191+05:30	1	9	\N
57	Regular Expense	Transport	50.00	CASH	2026-02-07	Rajkumar petrol	\N	\N	2026-02-27 16:02:30.935777+05:30	1	9	\N
58	Regular Expense	Petty Cash	400.00	CASH	2026-02-09	Lab Tea	\N	\N	2026-02-27 16:03:27.495968+05:30	1	9	\N
59	Regular Expense	Transport	1600.00	CASH	2026-02-11	Porter from Gqr	\N	\N	2026-02-27 16:04:11.587621+05:30	1	9	\N
60	Regular Expense	Labour	2000.00	CASH	2026-02-11	Rajkumar Adv	\N	\N	2026-02-27 16:04:38.014885+05:30	1	9	\N
61	Regular Expense	Labour	1500.00	CASH	2026-02-11	adv	\N	\N	2026-02-27 16:05:59.275611+05:30	1	9	\N
62	Regular Expense	OTHERS	8100.00	CASH	2026-02-13	Sharp Motor	\N	\N	2026-02-27 16:10:18.713746+05:30	1	9	\N
63	Regular Expense	OTHERS	1300.00	CASH	2026-02-13	fittings	\N	\N	2026-02-27 16:10:56.942196+05:30	1	9	\N
64	Regular Expense	OTHERS	200.00	CASH	2026-02-13	Drinking Water	\N	\N	2026-02-27 16:11:55.255266+05:30	1	9	\N
65	Regular Expense	Petty Cash	1400.00	CASH	2026-02-13	Curing Ooose	\N	\N	2026-02-27 16:12:35.49362+05:30	1	9	\N
66	Regular Expense	Labour	11100.00	CASH	2026-02-14	Weekly Payment	\N	\N	2026-02-27 16:13:15.396812+05:30	1	9	\N
67	Regular Expense	Labour	10700.00	CASH	2026-02-14	Fitter Lab	\N	\N	2026-02-27 16:13:52.632891+05:30	1	9	\N
68	Regular Expense	Labour	21000.00	CASH	2026-02-14	Prakash	\N	\N	2026-02-27 16:14:57.370216+05:30	1	9	\N
69	Regular Expense	OTHERS	500.00	CASH	2026-02-14	Selvam Electrician	\N	\N	2026-02-27 16:15:23.36522+05:30	1	9	\N
70	Regular Expense	Petty Cash	1250.00	CASH	2026-02-14	Kooni Saaku Curing	\N	\N	2026-02-27 16:15:58.89681+05:30	1	9	\N
71	Regular Expense	Transport	330.00	CASH	2026-02-14	Porter for Konni Saaku	\N	\N	2026-02-27 16:16:33.460639+05:30	1	9	\N
72	Regular Expense	Petty Cash	500.00	CASH	2026-02-16	Lab Tea	\N	\N	2026-02-27 16:18:37.071427+05:30	1	9	\N
73	Regular Expense	Material	150.00	CASH	2026-02-16	Blade	\N	\N	2026-02-27 16:19:06.128805+05:30	1	9	\N
75	Regular Expense	Labour	900.00	CASH	2026-02-21	Sudha payment	\N	\N	2026-02-27 16:24:48.99857+05:30	1	9	\N
76	Regular Expense	JCB	11100.00	CASH	2026-02-21	Earth work	\N	\N	2026-02-27 16:25:38.750764+05:30	1	9	\N
77	Regular Expense	Material	700.00	CASH	2026-02-27	Bond 7nos	\N	\N	2026-02-27 16:26:22.687712+05:30	1	9	\N
74	Regular Expense	Labour	18250.00	CASH	2026-02-21	Mason Lab	\N	\N	2026-02-27 16:23:25.767186+05:30	1	9	\N
78	Regular Expense	Petty Cash	2400.00	CASH	2026-02-21	Water for consoldilation	\N	\N	2026-02-27 16:26:58.755502+05:30	1	9	\N
79	Regular Expense	Transport	200.00	CASH	2026-02-23	Auto	\N	\N	2026-02-27 16:27:53.520469+05:30	1	9	\N
80	Regular Expense	Labour	1000.00	CASH	2026-02-25	lab adv	\N	\N	2026-02-27 16:28:47.663816+05:30	1	9	\N
81	Regular Expense	Labour	3000.00	CASH	2026-02-25	Ramesh adv	\N	\N	2026-02-27 16:29:18.455236+05:30	1	9	\N
82	Regular Expense	Material	550.00	CASH	2026-02-25	Nails	\N	\N	2026-02-27 16:30:39.33707+05:30	1	9	\N
84	Regular Expense	Labour	15000.00	CASH	2026-04-05		\N	\N	2026-04-05 21:13:04.577262+05:30	1	8	\N
\.


--
-- Data for Name: expense_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.expense_item (expense_item_id, item_name, quantity, measuring_unit, unit_price, total_price, expense_id, brand) FROM stdin;
1	sengal	20.00	Unit	500.00	10000.00	5	\N
2	Sengal	500.00	Unit	1000.00	500000.00	6	\N
3	Steel Bars	800.00	Unit	400.00	320000.00	7	\N
4	Binding Wire	100.00	Unit	300.00	30000.00	7	\N
5	Steel	100.00	Unit	15000.00	1500000.00	9	\N
6	Sengal	200.00	Unit	100.00	20000.00	12	\N
7	Electrical Cables	500.00	Unit	200.00	100000.00	13	\N
8	Switch Boards	100.00	Unit	800.00	80000.00	13	\N
9	Cement	1200.00	Unit	300.00	360000.00	14	\N
10	Sand	4.00	Unit	10000.00	40000.00	14	\N
11	Asian Paints	500.00	Unit	400.00	200000.00	15	\N
12	Interior Emulsion	250.00	Unit	200.00	50000.00	15	\N
13	Electrical	500.00	Unit	5000.00	2500000.00	16	\N
14	Green Paint	500.00	Unit	2000.00	1000000.00	17	\N
15	10mm	10.00	kg	5000.00	50000.00	24	\N
17	12mm	20.00	kg	2500.00	50000.00	26	\N
18	25mm	15.00	kg	5000.00	75000.00	26	\N
19	Monthly wages	1.00	unit	120000.00	120000.00	1	\N
20	Monthly wages for masons	1.00	unit	180000.00	180000.00	2	\N
21	Small tools and supplies	1.00	unit	15000.00	15000.00	3	\N
22	Site electricity charges	1.00	unit	42000.00	42000.00	4	\N
23	Helmets, boots, safety nets	1.00	unit	35000.00	35000.00	8	\N
24	Mason	1.00	unit	15000.00	15000.00	10	\N
25	Engineer salary	1.00	unit	60000.00	60000.00	11	\N
26	Tea	1.00	unit	2000.00	2000.00	18	\N
27	Others	1.00	unit	50000.00	50000.00	19	\N
28	Tea	1.00	unit	5000.00	5000.00	20	\N
29	Material	1.00	unit	20000.00	20000.00	27	\N
30	12mm	40.00	kg	5000.00	200000.00	28	TMT
31	Hollow Block	200.00	nos	500.00	100000.00	29	NG
32	Red	500.00	nos	100.00	50000.00	29	NG
33	Others	1.00	unit	25000.00	25000.00	30	\N
36	10mm	15.00	kg	1000.00	15000.00	31	TMT
37	12mm	20.00	kg	2000.00	40000.00	31	TMT
38	Primer	25.00	ltr	2600.00	65000.00	32	ASIAN
39	Fly Ash	102.00	nos	10.25	1045.50	33	\N
40	Others	1.00	unit	300.00	300.00	34	\N
41	Labour	1.00	unit	200.00	200.00	35	\N
42	Centring	1.00	unit	160.00	160.00	36	\N
44	Salary	1.00	unit	60000.00	60000.00	38	\N
45	Mason	1.00	unit	540.00	540.00	37	\N
46	Centring	1.00	unit	10000.00	10000.00	39	\N
47	Extra Work	1.00	unit	1750.00	1750.00	40	\N
48	Others	1.00	unit	100.00	100.00	41	\N
49	Extra Work	1.00	unit	1500.00	1500.00	42	\N
50	Other	1.00	unit	90.00	90.00	43	\N
52	Mason	1.00	unit	8700.00	8700.00	44	\N
53	Centring	1.00	unit	8250.00	8250.00	45	\N
54	10mm	1685.00	kg	79.00	133115.00	46	VIZAG
55	12mm	880.00	kg	75.25	66220.00	47	VIZAG
56	M-Sand	4.00	Units	4750.00	19000.00	48	\N
57	40 MM	2.00	Units	4500.00	9000.00	48	\N
58	Cement	60.00	Bags	290.00	17400.00	49	RAMCO
59	Cement	120.00	Bags	290.00	34800.00	50	RAMCO
60	M-Sand	8.00	Units	4750.00	38000.00	51	\N
61	20 MM	12.00	Units	4125.00	49500.00	52	\N
62	Cement	50.00	Bags	290.00	14500.00	53	RAMCO
63	Fly Ash	3000.00	nos	7.50	22500.00	54	\N
64	12mm	786.00	kg	77.60	60993.60	55	VIZAG
65	Extra Work	1.00	unit	15000.00	15000.00	56	\N
66	Labour	1.00	unit	50.00	50.00	57	\N
67	Tea	1.00	unit	400.00	400.00	58	\N
68	Material	1.00	unit	1600.00	1600.00	59	\N
69	Mason	1.00	unit	2000.00	2000.00	60	\N
70	Centring	1.00	unit	1500.00	1500.00	61	\N
71	Extra Work	1.00	unit	8100.00	8100.00	62	\N
72	Extra Work	1.00	unit	1300.00	1300.00	63	\N
73	Extra Work	1.00	unit	200.00	200.00	64	\N
74	Others	1.00	unit	1400.00	1400.00	65	\N
75	Mason	1.00	unit	11100.00	11100.00	66	\N
76	Centring	1.00	unit	10700.00	10700.00	67	\N
77	Concrete Group	1.00	unit	21000.00	21000.00	68	\N
78	Extra Work	1.00	unit	500.00	500.00	69	\N
79	Others	1.00	unit	1250.00	1250.00	70	\N
80	Other	1.00	unit	330.00	330.00	71	\N
81	Tea	1.00	unit	500.00	500.00	72	\N
82	Centring	1.00	unit	150.00	150.00	73	\N
83	Mason	1.00	unit	18250.00	18250.00	74	\N
84	Mason	1.00	unit	900.00	900.00	75	\N
85	JCB	1.00	unit	11100.00	11100.00	76	\N
86	Mason	1.00	unit	700.00	700.00	77	\N
87	Others	1.00	unit	2400.00	2400.00	78	\N
88	Labour	1.00	unit	200.00	200.00	79	\N
89	Labour	1.00	unit	1000.00	1000.00	80	\N
90	Centring	1.00	unit	3000.00	3000.00	81	\N
91	Centring	1.00	unit	550.00	550.00	82	\N
92	Concrete Group	1.00	unit	15000.00	15000.00	84	\N
\.


--
-- Data for Name: master_category; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.master_category (category_id, name, type, is_active) FROM stdin;
1	Steel	MATERIAL	t
4	Painting	MATERIAL	t
5	Plumbing	MATERIAL	t
6	Electrical	MATERIAL	t
8	Rental	EXPENSE	t
9	Transport	EXPENSE	t
10	Petty Cash	EXPENSE	t
11	TSK Expense	EXPENSE	t
3	Brick	MATERIAL	t
12	Material	EXPENSE	t
13	OTHERS	EXPENSE	t
14	Building Materials	MATERIAL	t
7	Labour	EXPENSE	t
15	JCB	EXPENSE	t
\.


--
-- Data for Name: payment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payment (payment_id, amount, payment_date, payment_mode, created_at, company_id, expense_id, project_id, vendor_id) FROM stdin;
1	100000.00	2025-03-15	UPI	2026-02-16 14:49:01.09337+05:30	1	13	1	7
2	400000.00	2025-02-15	BANK	2026-02-16 14:49:01.097239+05:30	1	14	4	1
3	150000.00	2026-02-02	CASH	2026-02-16 14:49:01.097239+05:30	1	\N	5	4
4	30000.00	2026-02-09	CASH	2026-02-16 14:49:01.097239+05:30	1	\N	5	5
5	200000.00	2025-03-10	BANK	2026-02-16 14:49:01.097239+05:30	1	7	1	4
6	1000000.00	2026-02-02	BANK	2026-02-16 14:49:01.097239+05:30	1	\N	5	7
7	195000.00	2026-02-07	CASH	2026-02-27 15:57:17.534267+05:30	1	\N	9	9
8	20000.00	2026-02-07	CASH	2026-02-27 16:00:48.623688+05:30	1	\N	9	10
9	50000.00	2026-02-14	CASH	2026-02-27 16:17:32.257024+05:30	1	\N	9	10
11	50000.00	2026-02-18	CASH	2026-02-27 16:22:31.183996+05:30	1	\N	9	11
12	50000.00	2026-03-12	CASH	2026-03-12 23:39:31.04074+05:30	1	\N	8	8
\.


--
-- Data for Name: sub_category; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sub_category (subcategory_id, name, default_unit, parent_category_id) FROM stdin;
1	8mm	kg	1
2	10mm	kg	1
3	12mm	kg	1
4	16mm	kg	1
5	20mm	kg	1
6	25mm	kg	1
7	32mm	kg	1
8	36mm	kg	1
24	Primer	ltr	4
25	Emulsion	ltr	4
26	Putty	ltr	4
27	White cement	ltr	4
28	Other	ltr	4
29	Fitting	nos	5
30	PVC	nos	5
31	UPVC	nos	5
32	CPVC	nos	5
33	Other	nos	5
34	Wires	nos	6
35	Switches	nos	6
36	Pipes	nos	6
42	Centring Rental	\N	8
43	JCB	\N	8
44	Bobcat	\N	8
45	Labour	\N	9
46	Material	\N	9
47	Other	\N	9
48	Tea	\N	10
49	Others	\N	10
50	Salary	\N	11
51	Others	\N	11
52	Withdrawal	\N	11
53	Chit	\N	11
79	Red	nos	3
80	Hollow Block	nos	3
81	Fly Ash	nos	3
84	Mason		12
85	Centring		12
86	Extra Work		13
93	M-Sand	Units	14
94	P-Sand	Units	14
95	20 MM	Units	14
96	40 MM	Units	14
97	Gravel	Units	14
98	Baby Chips	Units	14
99	Cement	Bags	14
100	Mason		7
101	Centring		7
102	Painter		7
103	Electrician		7
104	Plumber		7
105	Concrete Group		7
106	JCB		15
\.


--
-- Name: auth_group_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_group_id_seq', 1, false);


--
-- Name: auth_group_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_group_permissions_id_seq', 1, false);


--
-- Name: auth_permission_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_permission_id_seq', 60, true);


--
-- Name: client_payment_client_payment_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.client_payment_client_payment_id_seq1', 12, true);


--
-- Name: company_company_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.company_company_id_seq1', 2, true);


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.django_admin_log_id_seq', 1, false);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.django_content_type_id_seq', 15, true);


--
-- Name: django_migrations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.django_migrations_id_seq', 27, true);


--
-- Name: expense_expense_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.expense_expense_id_seq1', 84, true);


--
-- Name: expense_item_expense_item_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.expense_item_expense_item_id_seq1', 92, true);


--
-- Name: master_category_category_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.master_category_category_id_seq1', 15, true);


--
-- Name: payment_payment_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payment_payment_id_seq1', 12, true);


--
-- Name: project_project_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.project_project_id_seq1', 9, true);


--
-- Name: sub_category_subcategory_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sub_category_subcategory_id_seq1', 106, true);


--
-- Name: user_groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_groups_id_seq', 1, false);


--
-- Name: user_user_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_user_id_seq1', 7, true);


--
-- Name: user_user_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_user_permissions_id_seq', 1, false);


--
-- Name: vendor_vendor_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vendor_vendor_id_seq1', 12, true);


--
-- PostgreSQL database dump complete
--


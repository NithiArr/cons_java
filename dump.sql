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

ALTER TABLE IF EXISTS ONLY public.vendor DROP CONSTRAINT IF EXISTS vendor_company_id_68caa855_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.app_user_user_permissions DROP CONSTRAINT IF EXISTS user_user_permissions_user_id_ed4a47ea_fk_user_user_id;
ALTER TABLE IF EXISTS ONLY public.app_user_user_permissions DROP CONSTRAINT IF EXISTS user_user_permission_permission_id_9deb68a3_fk_auth_perm;
ALTER TABLE IF EXISTS ONLY public.app_user_groups DROP CONSTRAINT IF EXISTS user_groups_user_id_abaea130_fk_user_user_id;
ALTER TABLE IF EXISTS ONLY public.app_user_groups DROP CONSTRAINT IF EXISTS user_groups_group_id_b76f8aba_fk_auth_group_id;
ALTER TABLE IF EXISTS ONLY public.app_user DROP CONSTRAINT IF EXISTS user_company_id_99854d28_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.sub_category DROP CONSTRAINT IF EXISTS sub_category_parent_category_id_86f51c8e_fk_master_ca;
ALTER TABLE IF EXISTS ONLY public.project DROP CONSTRAINT IF EXISTS project_company_id_ada8e20e_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.payment DROP CONSTRAINT IF EXISTS payment_vendor_id_292bb080_fk_vendor_vendor_id;
ALTER TABLE IF EXISTS ONLY public.payment DROP CONSTRAINT IF EXISTS payment_project_id_c39fb853_fk_project_project_id;
ALTER TABLE IF EXISTS ONLY public.payment DROP CONSTRAINT IF EXISTS payment_expense_id_ebfa07c6_fk_expense_expense_id;
ALTER TABLE IF EXISTS ONLY public.payment DROP CONSTRAINT IF EXISTS payment_company_id_3d04cb29_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.expense DROP CONSTRAINT IF EXISTS expense_vendor_id_9319b97a_fk_vendor_vendor_id;
ALTER TABLE IF EXISTS ONLY public.expense DROP CONSTRAINT IF EXISTS expense_project_id_8ab671dd_fk_project_project_id;
ALTER TABLE IF EXISTS ONLY public.expense_item DROP CONSTRAINT IF EXISTS expense_item_expense_id_0a369916_fk_expense_expense_id;
ALTER TABLE IF EXISTS ONLY public.expense DROP CONSTRAINT IF EXISTS expense_company_id_7923445e_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.django_admin_log DROP CONSTRAINT IF EXISTS django_admin_log_user_id_c564eba6_fk_user_user_id;
ALTER TABLE IF EXISTS ONLY public.django_admin_log DROP CONSTRAINT IF EXISTS django_admin_log_content_type_id_c4bce8eb_fk_django_co;
ALTER TABLE IF EXISTS ONLY public.client_payment DROP CONSTRAINT IF EXISTS client_payment_project_id_7851b0e6_fk_project_project_id;
ALTER TABLE IF EXISTS ONLY public.client_payment DROP CONSTRAINT IF EXISTS client_payment_company_id_f0f47e8e_fk_company_company_id;
ALTER TABLE IF EXISTS ONLY public.auth_permission DROP CONSTRAINT IF EXISTS auth_permission_content_type_id_2f476e4b_fk_django_co;
ALTER TABLE IF EXISTS ONLY public.auth_group_permissions DROP CONSTRAINT IF EXISTS auth_group_permissions_group_id_b120cbf9_fk_auth_group_id;
ALTER TABLE IF EXISTS ONLY public.auth_group_permissions DROP CONSTRAINT IF EXISTS auth_group_permissio_permission_id_84c5c92e_fk_auth_perm;
DROP INDEX IF EXISTS public.vendor_company_id_68caa855;
DROP INDEX IF EXISTS public.user_user_permissions_user_id_ed4a47ea;
DROP INDEX IF EXISTS public.user_user_permissions_permission_id_9deb68a3;
DROP INDEX IF EXISTS public.user_groups_user_id_abaea130;
DROP INDEX IF EXISTS public.user_groups_group_id_b76f8aba;
DROP INDEX IF EXISTS public.user_email_54dc62b2_like;
DROP INDEX IF EXISTS public.user_company_id_99854d28;
DROP INDEX IF EXISTS public.sub_category_parent_category_id_86f51c8e;
DROP INDEX IF EXISTS public.project_company_id_ada8e20e;
DROP INDEX IF EXISTS public.payment_vendor_id_292bb080;
DROP INDEX IF EXISTS public.payment_project_id_c39fb853;
DROP INDEX IF EXISTS public.payment_expense_id_ebfa07c6;
DROP INDEX IF EXISTS public.payment_company_id_3d04cb29;
DROP INDEX IF EXISTS public.master_category_name_2479ba22_like;
DROP INDEX IF EXISTS public.expense_vendor_id_9319b97a;
DROP INDEX IF EXISTS public.expense_project_id_8ab671dd;
DROP INDEX IF EXISTS public.expense_item_expense_id_0a369916;
DROP INDEX IF EXISTS public.expense_company_id_7923445e;
DROP INDEX IF EXISTS public.django_session_session_key_c0390e0f_like;
DROP INDEX IF EXISTS public.django_session_expire_date_a5c62663;
DROP INDEX IF EXISTS public.django_admin_log_user_id_c564eba6;
DROP INDEX IF EXISTS public.django_admin_log_content_type_id_c4bce8eb;
DROP INDEX IF EXISTS public.client_payment_project_id_7851b0e6;
DROP INDEX IF EXISTS public.client_payment_company_id_f0f47e8e;
DROP INDEX IF EXISTS public.auth_permission_content_type_id_2f476e4b;
DROP INDEX IF EXISTS public.auth_group_permissions_permission_id_84c5c92e;
DROP INDEX IF EXISTS public.auth_group_permissions_group_id_b120cbf9;
DROP INDEX IF EXISTS public.auth_group_name_a6ea08ec_like;
ALTER TABLE IF EXISTS ONLY public.vendor DROP CONSTRAINT IF EXISTS vendor_pkey1;
ALTER TABLE IF EXISTS ONLY public.app_user_user_permissions DROP CONSTRAINT IF EXISTS user_user_permissions_user_id_permission_id_7dc6e2e0_uniq;
ALTER TABLE IF EXISTS ONLY public.app_user_user_permissions DROP CONSTRAINT IF EXISTS user_user_permissions_pkey;
ALTER TABLE IF EXISTS ONLY public.app_user DROP CONSTRAINT IF EXISTS user_pkey1;
ALTER TABLE IF EXISTS ONLY public.app_user_groups DROP CONSTRAINT IF EXISTS user_groups_user_id_group_id_40beef00_uniq;
ALTER TABLE IF EXISTS ONLY public.app_user_groups DROP CONSTRAINT IF EXISTS user_groups_pkey;
ALTER TABLE IF EXISTS ONLY public.app_user DROP CONSTRAINT IF EXISTS user_email_key1;
ALTER TABLE IF EXISTS ONLY public.sub_category DROP CONSTRAINT IF EXISTS sub_category_pkey1;
ALTER TABLE IF EXISTS ONLY public.project DROP CONSTRAINT IF EXISTS project_pkey1;
ALTER TABLE IF EXISTS ONLY public.payment DROP CONSTRAINT IF EXISTS payment_pkey1;
ALTER TABLE IF EXISTS ONLY public.master_category DROP CONSTRAINT IF EXISTS master_category_pkey1;
ALTER TABLE IF EXISTS ONLY public.master_category DROP CONSTRAINT IF EXISTS master_category_name_key1;
ALTER TABLE IF EXISTS ONLY public.expense DROP CONSTRAINT IF EXISTS expense_pkey1;
ALTER TABLE IF EXISTS ONLY public.expense_item DROP CONSTRAINT IF EXISTS expense_item_pkey1;
ALTER TABLE IF EXISTS ONLY public.django_session DROP CONSTRAINT IF EXISTS django_session_pkey;
ALTER TABLE IF EXISTS ONLY public.django_migrations DROP CONSTRAINT IF EXISTS django_migrations_pkey;
ALTER TABLE IF EXISTS ONLY public.django_content_type DROP CONSTRAINT IF EXISTS django_content_type_pkey;
ALTER TABLE IF EXISTS ONLY public.django_content_type DROP CONSTRAINT IF EXISTS django_content_type_app_label_model_76bd3d3b_uniq;
ALTER TABLE IF EXISTS ONLY public.django_admin_log DROP CONSTRAINT IF EXISTS django_admin_log_pkey;
ALTER TABLE IF EXISTS ONLY public.company DROP CONSTRAINT IF EXISTS company_pkey1;
ALTER TABLE IF EXISTS ONLY public.client_payment DROP CONSTRAINT IF EXISTS client_payment_pkey1;
ALTER TABLE IF EXISTS ONLY public.auth_permission DROP CONSTRAINT IF EXISTS auth_permission_pkey;
ALTER TABLE IF EXISTS ONLY public.auth_permission DROP CONSTRAINT IF EXISTS auth_permission_content_type_id_codename_01ab375a_uniq;
ALTER TABLE IF EXISTS ONLY public.auth_group DROP CONSTRAINT IF EXISTS auth_group_pkey;
ALTER TABLE IF EXISTS ONLY public.auth_group_permissions DROP CONSTRAINT IF EXISTS auth_group_permissions_pkey;
ALTER TABLE IF EXISTS ONLY public.auth_group_permissions DROP CONSTRAINT IF EXISTS auth_group_permissions_group_id_permission_id_0cd325b0_uniq;
ALTER TABLE IF EXISTS ONLY public.auth_group DROP CONSTRAINT IF EXISTS auth_group_name_key;
DROP TABLE IF EXISTS public.vendor;
DROP TABLE IF EXISTS public.sub_category;
DROP TABLE IF EXISTS public.project;
DROP TABLE IF EXISTS public.payment;
DROP TABLE IF EXISTS public.master_category;
DROP TABLE IF EXISTS public.expense_item;
DROP TABLE IF EXISTS public.expense;
DROP TABLE IF EXISTS public.django_session;
DROP TABLE IF EXISTS public.django_migrations;
DROP TABLE IF EXISTS public.django_content_type;
DROP TABLE IF EXISTS public.django_admin_log;
DROP TABLE IF EXISTS public.company;
DROP TABLE IF EXISTS public.client_payment;
DROP TABLE IF EXISTS public.auth_permission;
DROP TABLE IF EXISTS public.auth_group_permissions;
DROP TABLE IF EXISTS public.auth_group;
DROP TABLE IF EXISTS public.app_user_user_permissions;
DROP TABLE IF EXISTS public.app_user_groups;
DROP TABLE IF EXISTS public.app_user;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user (
    password character varying(255) NOT NULL,
    last_login timestamp with time zone,
    is_superuser boolean NOT NULL,
    first_name character varying(150) NOT NULL,
    last_name character varying(150) NOT NULL,
    is_staff boolean NOT NULL,
    is_active boolean NOT NULL,
    date_joined timestamp with time zone NOT NULL,
    user_id integer NOT NULL,
    email character varying(254) NOT NULL,
    name character varying(200) NOT NULL,
    role character varying(20) NOT NULL,
    company_id integer
);


--
-- Name: app_user_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user_groups (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    group_id integer NOT NULL
);


--
-- Name: app_user_user_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user_user_permissions (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    permission_id integer NOT NULL
);


--
-- Name: auth_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_group (
    id integer NOT NULL,
    name character varying(150) NOT NULL
);


--
-- Name: auth_group_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_group ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_group_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_group_permissions (
    id bigint NOT NULL,
    group_id integer NOT NULL,
    permission_id integer NOT NULL
);


--
-- Name: auth_group_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_group_permissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_group_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    content_type_id integer NOT NULL,
    codename character varying(100) NOT NULL
);


--
-- Name: auth_permission_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_permission ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: client_payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_payment (
    client_payment_id integer NOT NULL,
    amount numeric(15,2) NOT NULL,
    payment_date date NOT NULL,
    payment_mode character varying(20) NOT NULL,
    reference_number character varying(100),
    remarks text,
    created_at timestamp with time zone NOT NULL,
    company_id integer NOT NULL,
    project_id integer NOT NULL
);


--
-- Name: client_payment_client_payment_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.client_payment ALTER COLUMN client_payment_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.client_payment_client_payment_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: company; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company (
    company_id integer NOT NULL,
    name character varying(200) NOT NULL,
    address text,
    phone character varying(20),
    email character varying(120),
    created_at timestamp with time zone NOT NULL
);


--
-- Name: company_company_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.company ALTER COLUMN company_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.company_company_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_admin_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_admin_log (
    id integer NOT NULL,
    action_time timestamp with time zone NOT NULL,
    object_id text,
    object_repr character varying(200) NOT NULL,
    action_flag smallint NOT NULL,
    change_message text NOT NULL,
    content_type_id integer,
    user_id integer NOT NULL,
    CONSTRAINT django_admin_log_action_flag_check CHECK ((action_flag >= 0))
);


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.django_admin_log ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.django_admin_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_content_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_content_type (
    id integer NOT NULL,
    app_label character varying(100) NOT NULL,
    model character varying(100) NOT NULL
);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.django_content_type ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.django_content_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_migrations (
    id bigint NOT NULL,
    app character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    applied timestamp with time zone NOT NULL
);


--
-- Name: django_migrations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.django_migrations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.django_migrations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_session (
    session_key character varying(40) NOT NULL,
    session_data text NOT NULL,
    expire_date timestamp with time zone NOT NULL
);


--
-- Name: expense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense (
    expense_id integer NOT NULL,
    expense_type character varying(100) NOT NULL,
    category character varying(100),
    amount numeric(15,2) NOT NULL,
    payment_mode character varying(20) NOT NULL,
    expense_date date NOT NULL,
    description text,
    invoice_number character varying(100),
    bill_url character varying(500),
    created_at timestamp with time zone NOT NULL,
    company_id integer NOT NULL,
    project_id integer NOT NULL,
    vendor_id integer
);


--
-- Name: expense_expense_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.expense ALTER COLUMN expense_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.expense_expense_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: expense_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense_item (
    expense_item_id integer NOT NULL,
    item_name character varying(200) NOT NULL,
    quantity numeric(10,2) NOT NULL,
    measuring_unit character varying(20) NOT NULL,
    unit_price numeric(15,2) NOT NULL,
    total_price numeric(15,2) NOT NULL,
    expense_id integer NOT NULL
);


--
-- Name: expense_item_expense_item_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.expense_item ALTER COLUMN expense_item_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.expense_item_expense_item_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: master_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.master_category (
    category_id integer NOT NULL,
    name character varying(100) NOT NULL,
    type character varying(50) NOT NULL,
    is_active boolean NOT NULL
);


--
-- Name: master_category_category_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.master_category ALTER COLUMN category_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.master_category_category_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment (
    payment_id integer NOT NULL,
    amount numeric(15,2) NOT NULL,
    payment_date date NOT NULL,
    payment_mode character varying(20) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    company_id integer NOT NULL,
    expense_id integer,
    project_id integer NOT NULL,
    vendor_id integer NOT NULL
);


--
-- Name: payment_payment_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.payment ALTER COLUMN payment_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.payment_payment_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project (
    project_id integer NOT NULL,
    name character varying(200) NOT NULL,
    location character varying(300),
    start_date date,
    end_date date,
    budget numeric(15,2) NOT NULL,
    status character varying(20) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    company_id integer NOT NULL
);


--
-- Name: project_project_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.project ALTER COLUMN project_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.project_project_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sub_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sub_category (
    subcategory_id integer NOT NULL,
    name character varying(100) NOT NULL,
    default_unit character varying(20),
    parent_category_id integer NOT NULL
);


--
-- Name: sub_category_subcategory_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.sub_category ALTER COLUMN subcategory_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.sub_category_subcategory_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.app_user_groups ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.user_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_user_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.app_user ALTER COLUMN user_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.user_user_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_user_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.app_user_user_permissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.user_user_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vendor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vendor (
    vendor_id integer NOT NULL,
    name character varying(200) NOT NULL,
    phone character varying(20),
    email character varying(120),
    gst_number character varying(50),
    created_at timestamp with time zone NOT NULL,
    company_id integer NOT NULL
);


--
-- Name: vendor_vendor_id_seq1; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.vendor ALTER COLUMN vendor_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.vendor_vendor_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user (password, last_login, is_superuser, first_name, last_name, is_staff, is_active, date_joined, user_id, email, name, role, company_id) FROM stdin;
scrypt:32768:8:1$IeblpDGsttALYNES$cb21a1a72192bd2e98f497c78e1ca807bed43af57d0214eee157a8708856512780b795a830f8cf9a9963ba855c1cc7a367cef28579c70e8215ecfb56f40a4273	\N	f			f	t	2026-01-31 22:15:22+05:30	4	manager@tsconst.com	Project Manager	MANAGER	1
scrypt:32768:8:1$EnhLyTIRHqkeMdnB$689c8dcf4589e203f075284423d5284ee765b66651e1054250f7472d9aac3041b180d81b0b24402fb66ed545186556509f98af3c2769a827860f2d48d5693a21	\N	f			f	t	2026-01-31 22:15:22+05:30	5	engineer@tsconst.com	Site Engineer	ENGINEER	1
pbkdf2_sha256$1000000$aIe8w4LTEu46Y73LrL0O4N$6ueByOo4SL/o6e1rWgmI9+akkgb6rdIV7B04VmxAgx0=	\N	f			f	t	2026-01-31 22:15:22+05:30	1	accountant@tsconst.com	Accountant	ACCOUNTANT	1
pbkdf2_sha256$1000000$qcwe2201skGXgdCHa2NXIN$cHRPHPZBPrClf5KnqhCEYyHyZwmWhsb0blERmMlqKbY=	2026-02-16 15:50:55.275782+05:30	t			t	t	2026-01-31 22:15:22+05:30	2	nithiarrvind@gmail.com	Nithi	OWNER	1
pbkdf2_sha256$1000000$hmKKCuuXHiM1H46eDTx73U$AhdFDpDO/SixH7hgooEqWLl5kEBufgQraapHaV5eQz4=	2026-02-16 15:51:38.128817+05:30	f			t	t	2026-01-31 22:15:22+05:30	3	admin@sorgavasal.com	Admin User	ADMIN	1
\.


--
-- Data for Name: app_user_groups; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user_groups (id, user_id, group_id) FROM stdin;
\.


--
-- Data for Name: app_user_user_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user_user_permissions (id, user_id, permission_id) FROM stdin;
\.


--
-- Data for Name: auth_group; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_group (id, name) FROM stdin;
\.


--
-- Data for Name: auth_group_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_group_permissions (id, group_id, permission_id) FROM stdin;
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
-- Data for Name: client_payment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_payment (client_payment_id, amount, payment_date, payment_mode, reference_number, remarks, created_at, company_id, project_id) FROM stdin;
1	4000000.00	2025-01-05	BANK	CHQ-002345	Advance payment	2026-02-16 14:49:01.097239+05:30	1	4
2	3000000.00	2024-12-30	BANK	CHQ-000123	Final payment on completion	2026-02-16 14:49:01.097239+05:30	1	2
3	3500000.00	2025-02-25	BANK	CHQ-002678	Progress payment	2026-02-16 14:49:01.097239+05:30	1	4
4	2500000.00	2026-02-02	BANK	\N	\N	2026-02-16 14:49:01.097239+05:30	1	5
5	2500000.00	2025-02-25	BANK	NEFT-789012	Booking amount	2026-02-16 14:49:01.103043+05:30	1	1
6	1500000.00	2025-03-20	BANK	NEFT-789345	Construction start payment	2026-02-16 14:49:01.103329+05:30	1	1
\.


--
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.company (company_id, name, address, phone, email, created_at) FROM stdin;
1	Sorgavasal	123 Construction St, Chennai	9876543210	info@sorgavasal.com	2026-02-16 14:49:01.018473+05:30
2	TS	21 B, Type II Quaters,	09442052861	ts@gmail.com	2026-02-16 14:49:01.018473+05:30
\.


--
-- Data for Name: django_admin_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_admin_log (id, action_time, object_id, object_repr, action_flag, change_message, content_type_id, user_id) FROM stdin;
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
\.


--
-- Data for Name: django_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_session (session_key, session_data, expire_date) FROM stdin;
4ukqytsgr69tv46elntshlefkkvf7zu3	.eJxVjMsOwiAQRf-FtSHQmfJw6d5vIAwDUjU0Ke3K-O_apAvd3nPOfYkQt7WGreclTCzOAsTpd6OYHrntgO-x3WaZ5rYuE8ldkQft8jpzfl4O9--gxl6_tfGOY0FjB80A1pKxWmXkkSB5igOqQmgLK4faj5i0RV-8I-TCCRDE-wPRezeF:1vrvjW:8TDVX9D-UNisK-_QFl9hZjDU08rxfitlSW8-DKL7H44	2026-03-02 15:51:38.130137+05:30
\.


--
-- Data for Name: expense; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.expense (expense_id, expense_type, category, amount, payment_mode, expense_date, description, invoice_number, bill_url, created_at, company_id, project_id, vendor_id) FROM stdin;
1	Regular Expense	Skilled Labor	120000.00	BANK	2025-02-28	Monthly wages	\N	\N	2026-02-16 14:49:01.07332+05:30	1	1	\N
2	Regular Expense	Skilled Labor	180000.00	BANK	2025-01-31	Monthly wages for masons	\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
3	Regular Expense	Miscellaneous	15000.00	CASH	2025-02-10	Small tools and supplies	\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
4	Regular Expense	Electricity Bill	42000.00	BANK	2025-02-20	Site electricity charges	\N	\N	2026-02-16 14:49:01.07432+05:30	1	4	\N
5	Material Purchase	Sengal	10000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	5	5
6	Material Purchase	Sengal	500000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	1	5
7	Material Purchase	Steel Bars	350000.00	CREDIT	2025-02-20	Invoice #INV-2025-010	INV-2025-010	\N	2026-02-16 14:49:01.07432+05:30	1	1	4
8	Regular Expense	Safety Equipment	35000.00	UPI	2025-03-05	Helmets, boots, safety nets	\N	\N	2026-02-16 14:49:01.07432+05:30	1	1	\N
9	Material Purchase	Steel Bars	1500000.00	CREDIT	2026-02-02	Invoice #INV-2025-002	INV-2025-002	\N	2026-02-16 14:49:01.07432+05:30	1	5	4
10	Regular Expense	Skilled Labor	15000.00	CASH	2026-02-02	Mason	\N	\N	2026-02-16 14:49:01.07432+05:30	1	5	\N
11	Regular Expense	Site Supervision	60000.00	BANK	2025-03-01	Engineer salary	\N	\N	2026-02-16 14:49:01.07432+05:30	1	1	\N
12	Material Purchase	Sengal	20000.00	CREDIT	2026-02-09	Invoice #		\N	2026-02-16 14:49:01.07432+05:30	1	5	5
13	Material Purchase	Electrical Cables	180000.00	CREDIT	2025-03-01	Invoice #INV-2025-011	INV-2025-011	\N	2026-02-16 14:49:01.07432+05:30	1	1	7
14	Material Purchase	Cement	400000.00	CREDIT	2025-01-10	Invoice #INV-2025-020	INV-2025-020	\N	2026-02-16 14:49:01.07432+05:30	1	4	1
15	Material Purchase	Asian Paints	250000.00	CREDIT	2025-02-15	Invoice #INV-2025-021	INV-2025-021	\N	2026-02-16 14:49:01.07432+05:30	1	4	8
16	Material Purchase	Electrical Cables	2500000.00	CREDIT	2026-02-02	Invoice #INV-2025-010	INV-2025-010	\N	2026-02-16 14:49:01.07432+05:30	1	5	7
17	Material Purchase	Asian Paints	1000000.00	CASH	2026-02-02	Invoice #inv 100	inv 100	\N	2026-02-16 14:49:01.07432+05:30	1	5	8
18	Regular Expense	Petty Cash	2000.00	BANK	2026-02-16	Tea	\N	\N	2026-02-16 20:36:36.82613+05:30	1	5	\N
19	Regular Expense	Petty Cash	50000.00	CASH	2026-02-16	Others	\N	\N	2026-02-16 21:02:47.176582+05:30	1	5	\N
20	Regular Expense	Petty Cash	5000.00	CASH	2026-02-15	Tea	\N	\N	2026-02-16 21:03:12.734585+05:30	1	5	\N
24	Material Purchase	Steel	50000.00	CASH	2026-02-16	Invoice #		\N	2026-02-16 21:17:34.989448+05:30	1	5	7
25	Material Purchase	Steel	105000.00	CREDIT	2026-02-16	Invoice #		\N	2026-02-16 21:18:01.305537+05:30	1	5	4
26	Material Purchase	Steel	125000.00	CASH	2026-02-16	Invoice #		\N	2026-02-16 21:48:12.779012+05:30	1	5	4
\.


--
-- Data for Name: expense_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.expense_item (expense_item_id, item_name, quantity, measuring_unit, unit_price, total_price, expense_id) FROM stdin;
1	sengal	20.00	Unit	500.00	10000.00	5
2	Sengal	500.00	Unit	1000.00	500000.00	6
3	Steel Bars	800.00	Unit	400.00	320000.00	7
4	Binding Wire	100.00	Unit	300.00	30000.00	7
5	Steel	100.00	Unit	15000.00	1500000.00	9
6	Sengal	200.00	Unit	100.00	20000.00	12
7	Electrical Cables	500.00	Unit	200.00	100000.00	13
8	Switch Boards	100.00	Unit	800.00	80000.00	13
9	Cement	1200.00	Unit	300.00	360000.00	14
10	Sand	4.00	Unit	10000.00	40000.00	14
11	Asian Paints	500.00	Unit	400.00	200000.00	15
12	Interior Emulsion	250.00	Unit	200.00	50000.00	15
13	Electrical	500.00	Unit	5000.00	2500000.00	16
14	Green Paint	500.00	Unit	2000.00	1000000.00	17
15	10mm	10.00	kg	5000.00	50000.00	24
16	25mm	15.00	kg	7000.00	105000.00	25
17	12mm	20.00	kg	2500.00	50000.00	26
18	25mm	15.00	kg	5000.00	75000.00	26
\.


--
-- Data for Name: master_category; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.master_category (category_id, name, type, is_active) FROM stdin;
1	Steel	MATERIAL	t
2	Building Materials	MATERIAL	t
3	Brick	MATERIAL	t
4	Painting	MATERIAL	t
5	Plumbing	MATERIAL	t
6	Electrical	MATERIAL	t
7	Labour	EXPENSE	t
8	Rental	EXPENSE	t
9	Transport	EXPENSE	t
10	Petty Cash	EXPENSE	t
11	TSK Expense	EXPENSE	t
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
18	Red Brick-wirecut	nos	3
19	Red Brick-chamber	nos	3
20	Fly ash	nos	3
21	Solid block (4", 6", 12")	nos	3
22	AAC block	nos	3
23	Hollow block	nos	3
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
37	Mason	\N	7
38	Centring	\N	7
39	Painter	\N	7
40	Electrician	\N	7
41	Plumber	\N	7
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
54	Cement	unit	2
55	M-sand	kg	2
56	P-sand	kg	2
57	20mm Jally	unit	2
58	40mm Jally	unit	2
59	Gravel	unit	2
60	Baby Chips	unit	2
61	Wet Mix	unit	2
62	Others	unit	2
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

SELECT pg_catalog.setval('public.client_payment_client_payment_id_seq1', 1, false);


--
-- Name: company_company_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.company_company_id_seq1', 1, false);


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

SELECT pg_catalog.setval('public.django_migrations_id_seq', 25, true);


--
-- Name: expense_expense_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.expense_expense_id_seq1', 26, true);


--
-- Name: expense_item_expense_item_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.expense_item_expense_item_id_seq1', 18, true);


--
-- Name: master_category_category_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.master_category_category_id_seq1', 1, false);


--
-- Name: payment_payment_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payment_payment_id_seq1', 1, false);


--
-- Name: project_project_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.project_project_id_seq1', 1, false);


--
-- Name: sub_category_subcategory_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sub_category_subcategory_id_seq1', 1, false);


--
-- Name: user_groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_groups_id_seq', 1, false);


--
-- Name: user_user_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_user_id_seq1', 1, false);


--
-- Name: user_user_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_user_permissions_id_seq', 1, false);


--
-- Name: vendor_vendor_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vendor_vendor_id_seq1', 1, false);


--
-- Name: auth_group auth_group_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group
    ADD CONSTRAINT auth_group_name_key UNIQUE (name);


--
-- Name: auth_group_permissions auth_group_permissions_group_id_permission_id_0cd325b0_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_group_id_permission_id_0cd325b0_uniq UNIQUE (group_id, permission_id);


--
-- Name: auth_group_permissions auth_group_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_pkey PRIMARY KEY (id);


--
-- Name: auth_group auth_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group
    ADD CONSTRAINT auth_group_pkey PRIMARY KEY (id);


--
-- Name: auth_permission auth_permission_content_type_id_codename_01ab375a_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_codename_01ab375a_uniq UNIQUE (content_type_id, codename);


--
-- Name: auth_permission auth_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_pkey PRIMARY KEY (id);


--
-- Name: client_payment client_payment_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_payment
    ADD CONSTRAINT client_payment_pkey1 PRIMARY KEY (client_payment_id);


--
-- Name: company company_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey1 PRIMARY KEY (company_id);


--
-- Name: django_admin_log django_admin_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_pkey PRIMARY KEY (id);


--
-- Name: django_content_type django_content_type_app_label_model_76bd3d3b_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_content_type
    ADD CONSTRAINT django_content_type_app_label_model_76bd3d3b_uniq UNIQUE (app_label, model);


--
-- Name: django_content_type django_content_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_content_type
    ADD CONSTRAINT django_content_type_pkey PRIMARY KEY (id);


--
-- Name: django_migrations django_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_migrations
    ADD CONSTRAINT django_migrations_pkey PRIMARY KEY (id);


--
-- Name: django_session django_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_session
    ADD CONSTRAINT django_session_pkey PRIMARY KEY (session_key);


--
-- Name: expense_item expense_item_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_item
    ADD CONSTRAINT expense_item_pkey1 PRIMARY KEY (expense_item_id);


--
-- Name: expense expense_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_pkey1 PRIMARY KEY (expense_id);


--
-- Name: master_category master_category_name_key1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master_category
    ADD CONSTRAINT master_category_name_key1 UNIQUE (name);


--
-- Name: master_category master_category_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master_category
    ADD CONSTRAINT master_category_pkey1 PRIMARY KEY (category_id);


--
-- Name: payment payment_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey1 PRIMARY KEY (payment_id);


--
-- Name: project project_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey1 PRIMARY KEY (project_id);


--
-- Name: sub_category sub_category_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sub_category
    ADD CONSTRAINT sub_category_pkey1 PRIMARY KEY (subcategory_id);


--
-- Name: app_user user_email_key1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT user_email_key1 UNIQUE (email);


--
-- Name: app_user_groups user_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_groups
    ADD CONSTRAINT user_groups_pkey PRIMARY KEY (id);


--
-- Name: app_user_groups user_groups_user_id_group_id_40beef00_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_groups
    ADD CONSTRAINT user_groups_user_id_group_id_40beef00_uniq UNIQUE (user_id, group_id);


--
-- Name: app_user user_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT user_pkey1 PRIMARY KEY (user_id);


--
-- Name: app_user_user_permissions user_user_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_user_permissions
    ADD CONSTRAINT user_user_permissions_pkey PRIMARY KEY (id);


--
-- Name: app_user_user_permissions user_user_permissions_user_id_permission_id_7dc6e2e0_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_user_permissions
    ADD CONSTRAINT user_user_permissions_user_id_permission_id_7dc6e2e0_uniq UNIQUE (user_id, permission_id);


--
-- Name: vendor vendor_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendor
    ADD CONSTRAINT vendor_pkey1 PRIMARY KEY (vendor_id);


--
-- Name: auth_group_name_a6ea08ec_like; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_group_name_a6ea08ec_like ON public.auth_group USING btree (name varchar_pattern_ops);


--
-- Name: auth_group_permissions_group_id_b120cbf9; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_group_permissions_group_id_b120cbf9 ON public.auth_group_permissions USING btree (group_id);


--
-- Name: auth_group_permissions_permission_id_84c5c92e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_group_permissions_permission_id_84c5c92e ON public.auth_group_permissions USING btree (permission_id);


--
-- Name: auth_permission_content_type_id_2f476e4b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_permission_content_type_id_2f476e4b ON public.auth_permission USING btree (content_type_id);


--
-- Name: client_payment_company_id_f0f47e8e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX client_payment_company_id_f0f47e8e ON public.client_payment USING btree (company_id);


--
-- Name: client_payment_project_id_7851b0e6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX client_payment_project_id_7851b0e6 ON public.client_payment USING btree (project_id);


--
-- Name: django_admin_log_content_type_id_c4bce8eb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_admin_log_content_type_id_c4bce8eb ON public.django_admin_log USING btree (content_type_id);


--
-- Name: django_admin_log_user_id_c564eba6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_admin_log_user_id_c564eba6 ON public.django_admin_log USING btree (user_id);


--
-- Name: django_session_expire_date_a5c62663; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_session_expire_date_a5c62663 ON public.django_session USING btree (expire_date);


--
-- Name: django_session_session_key_c0390e0f_like; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_session_session_key_c0390e0f_like ON public.django_session USING btree (session_key varchar_pattern_ops);


--
-- Name: expense_company_id_7923445e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expense_company_id_7923445e ON public.expense USING btree (company_id);


--
-- Name: expense_item_expense_id_0a369916; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expense_item_expense_id_0a369916 ON public.expense_item USING btree (expense_id);


--
-- Name: expense_project_id_8ab671dd; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expense_project_id_8ab671dd ON public.expense USING btree (project_id);


--
-- Name: expense_vendor_id_9319b97a; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expense_vendor_id_9319b97a ON public.expense USING btree (vendor_id);


--
-- Name: master_category_name_2479ba22_like; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX master_category_name_2479ba22_like ON public.master_category USING btree (name varchar_pattern_ops);


--
-- Name: payment_company_id_3d04cb29; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX payment_company_id_3d04cb29 ON public.payment USING btree (company_id);


--
-- Name: payment_expense_id_ebfa07c6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX payment_expense_id_ebfa07c6 ON public.payment USING btree (expense_id);


--
-- Name: payment_project_id_c39fb853; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX payment_project_id_c39fb853 ON public.payment USING btree (project_id);


--
-- Name: payment_vendor_id_292bb080; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX payment_vendor_id_292bb080 ON public.payment USING btree (vendor_id);


--
-- Name: project_company_id_ada8e20e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX project_company_id_ada8e20e ON public.project USING btree (company_id);


--
-- Name: sub_category_parent_category_id_86f51c8e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX sub_category_parent_category_id_86f51c8e ON public.sub_category USING btree (parent_category_id);


--
-- Name: user_company_id_99854d28; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_company_id_99854d28 ON public.app_user USING btree (company_id);


--
-- Name: user_email_54dc62b2_like; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_email_54dc62b2_like ON public.app_user USING btree (email varchar_pattern_ops);


--
-- Name: user_groups_group_id_b76f8aba; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_groups_group_id_b76f8aba ON public.app_user_groups USING btree (group_id);


--
-- Name: user_groups_user_id_abaea130; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_groups_user_id_abaea130 ON public.app_user_groups USING btree (user_id);


--
-- Name: user_user_permissions_permission_id_9deb68a3; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_user_permissions_permission_id_9deb68a3 ON public.app_user_user_permissions USING btree (permission_id);


--
-- Name: user_user_permissions_user_id_ed4a47ea; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_user_permissions_user_id_ed4a47ea ON public.app_user_user_permissions USING btree (user_id);


--
-- Name: vendor_company_id_68caa855; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vendor_company_id_68caa855 ON public.vendor USING btree (company_id);


--
-- Name: auth_group_permissions auth_group_permissio_permission_id_84c5c92e_fk_auth_perm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group_permissions
    ADD CONSTRAINT auth_group_permissio_permission_id_84c5c92e_fk_auth_perm FOREIGN KEY (permission_id) REFERENCES public.auth_permission(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_group_permissions auth_group_permissions_group_id_b120cbf9_fk_auth_group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_group_id_b120cbf9_fk_auth_group_id FOREIGN KEY (group_id) REFERENCES public.auth_group(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_permission auth_permission_content_type_id_2f476e4b_fk_django_co; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_2f476e4b_fk_django_co FOREIGN KEY (content_type_id) REFERENCES public.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: client_payment client_payment_company_id_f0f47e8e_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_payment
    ADD CONSTRAINT client_payment_company_id_f0f47e8e_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: client_payment client_payment_project_id_7851b0e6_fk_project_project_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_payment
    ADD CONSTRAINT client_payment_project_id_7851b0e6_fk_project_project_id FOREIGN KEY (project_id) REFERENCES public.project(project_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: django_admin_log django_admin_log_content_type_id_c4bce8eb_fk_django_co; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_content_type_id_c4bce8eb_fk_django_co FOREIGN KEY (content_type_id) REFERENCES public.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: django_admin_log django_admin_log_user_id_c564eba6_fk_user_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_user_id_c564eba6_fk_user_user_id FOREIGN KEY (user_id) REFERENCES public.app_user(user_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expense expense_company_id_7923445e_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_company_id_7923445e_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expense_item expense_item_expense_id_0a369916_fk_expense_expense_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_item
    ADD CONSTRAINT expense_item_expense_id_0a369916_fk_expense_expense_id FOREIGN KEY (expense_id) REFERENCES public.expense(expense_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expense expense_project_id_8ab671dd_fk_project_project_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_project_id_8ab671dd_fk_project_project_id FOREIGN KEY (project_id) REFERENCES public.project(project_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: expense expense_vendor_id_9319b97a_fk_vendor_vendor_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_vendor_id_9319b97a_fk_vendor_vendor_id FOREIGN KEY (vendor_id) REFERENCES public.vendor(vendor_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: payment payment_company_id_3d04cb29_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_company_id_3d04cb29_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: payment payment_expense_id_ebfa07c6_fk_expense_expense_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_expense_id_ebfa07c6_fk_expense_expense_id FOREIGN KEY (expense_id) REFERENCES public.expense(expense_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: payment payment_project_id_c39fb853_fk_project_project_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_project_id_c39fb853_fk_project_project_id FOREIGN KEY (project_id) REFERENCES public.project(project_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: payment payment_vendor_id_292bb080_fk_vendor_vendor_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_vendor_id_292bb080_fk_vendor_vendor_id FOREIGN KEY (vendor_id) REFERENCES public.vendor(vendor_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: project project_company_id_ada8e20e_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_company_id_ada8e20e_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: sub_category sub_category_parent_category_id_86f51c8e_fk_master_ca; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sub_category
    ADD CONSTRAINT sub_category_parent_category_id_86f51c8e_fk_master_ca FOREIGN KEY (parent_category_id) REFERENCES public.master_category(category_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: app_user user_company_id_99854d28_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT user_company_id_99854d28_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: app_user_groups user_groups_group_id_b76f8aba_fk_auth_group_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_groups
    ADD CONSTRAINT user_groups_group_id_b76f8aba_fk_auth_group_id FOREIGN KEY (group_id) REFERENCES public.auth_group(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: app_user_groups user_groups_user_id_abaea130_fk_user_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_groups
    ADD CONSTRAINT user_groups_user_id_abaea130_fk_user_user_id FOREIGN KEY (user_id) REFERENCES public.app_user(user_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: app_user_user_permissions user_user_permission_permission_id_9deb68a3_fk_auth_perm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_user_permissions
    ADD CONSTRAINT user_user_permission_permission_id_9deb68a3_fk_auth_perm FOREIGN KEY (permission_id) REFERENCES public.auth_permission(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: app_user_user_permissions user_user_permissions_user_id_ed4a47ea_fk_user_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user_user_permissions
    ADD CONSTRAINT user_user_permissions_user_id_ed4a47ea_fk_user_user_id FOREIGN KEY (user_id) REFERENCES public.app_user(user_id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: vendor vendor_company_id_68caa855_fk_company_company_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendor
    ADD CONSTRAINT vendor_company_id_68caa855_fk_company_company_id FOREIGN KEY (company_id) REFERENCES public.company(company_id) DEFERRABLE INITIALLY DEFERRED;


--
-- PostgreSQL database dump complete
--


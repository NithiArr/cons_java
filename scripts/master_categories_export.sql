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
-- Data for Name: master_category; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (1, 'Steel', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (4, 'Painting', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (5, 'Plumbing', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (6, 'Electrical', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (8, 'Rental', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (9, 'Transport', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (10, 'Petty Cash', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (11, 'TSK Expense', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (3, 'Brick', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (12, 'Material', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (13, 'OTHERS', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (14, 'Building Materials', 'MATERIAL', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (7, 'Labour', 'EXPENSE', true);
INSERT INTO public.master_category (category_id, name, type, is_active) VALUES (15, 'JCB', 'EXPENSE', true);


--
-- Data for Name: sub_category; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (1, '8mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (2, '10mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (3, '12mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (4, '16mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (5, '20mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (6, '25mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (7, '32mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (8, '36mm', 'kg', 1);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (24, 'Primer', 'ltr', 4);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (25, 'Emulsion', 'ltr', 4);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (26, 'Putty', 'ltr', 4);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (27, 'White cement', 'ltr', 4);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (28, 'Other', 'ltr', 4);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (29, 'Fitting', 'nos', 5);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (30, 'PVC', 'nos', 5);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (31, 'UPVC', 'nos', 5);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (32, 'CPVC', 'nos', 5);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (33, 'Other', 'nos', 5);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (34, 'Wires', 'nos', 6);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (35, 'Switches', 'nos', 6);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (36, 'Pipes', 'nos', 6);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (42, 'Centring Rental', NULL, 8);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (43, 'JCB', NULL, 8);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (44, 'Bobcat', NULL, 8);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (45, 'Labour', NULL, 9);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (46, 'Material', NULL, 9);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (47, 'Other', NULL, 9);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (48, 'Tea', NULL, 10);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (49, 'Others', NULL, 10);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (50, 'Salary', NULL, 11);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (51, 'Others', NULL, 11);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (52, 'Withdrawal', NULL, 11);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (53, 'Chit', NULL, 11);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (79, 'Red', 'nos', 3);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (80, 'Hollow Block', 'nos', 3);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (81, 'Fly Ash', 'nos', 3);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (84, 'Mason', '', 12);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (85, 'Centring', '', 12);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (93, 'M-Sand', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (94, 'P-Sand', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (95, '20 MM', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (96, '40 MM', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (97, 'Gravel', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (98, 'Baby Chips', 'Units', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (99, 'Cement', 'Bags', 14);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (100, 'Mason', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (101, 'Centring', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (102, 'Painter', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (103, 'Electrician', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (104, 'Plumber', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (105, 'Concrete Group', '', 7);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (106, 'JCB', '', 15);
INSERT INTO public.sub_category (subcategory_id, name, default_unit, parent_category_id) VALUES (109, 'Extra Work', '', 13);


--
-- Name: master_category_category_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.master_category_category_id_seq1', 15, true);


--
-- Name: sub_category_subcategory_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sub_category_subcategory_id_seq1', 109, true);


--
-- PostgreSQL database dump complete
--


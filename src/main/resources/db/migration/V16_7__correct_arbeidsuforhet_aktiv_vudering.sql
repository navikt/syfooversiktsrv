UPDATE person_oversikt_status
SET arbeidsuforhet_aktiv_vurdering = false,
    sist_endret=now()
WHERE uuid in
      (
       '88e6749c-4b82-42db-83f2-2d533aeb9410',
       '4960ef4b-78e8-4915-8732-a9855e819bf1',
       '099a06f1-4450-48fe-b70f-c8fcac6bc76f',
       '3918809e-e9ec-4737-bec7-a84529c506d0',
       'bc6869d1-b7d6-40b6-a92e-bc6a0310a8da',
       '238ee8ff-3982-487f-b256-bbb7e4583345',
       '64ac3e11-9195-467b-a64d-aae92333a9a3',
       '7f3c997b-4790-4990-940c-9094a7a8ceb6',
       '6c4663e6-17ad-44b3-a187-deaff500453e',
       '518fedd4-4049-4381-9993-a580f45b99a5',
       '5530f4f0-bbaa-44d5-a231-c2676c6b617b',
       '0b57fad9-29ea-4455-a4f0-4934c1432832',
       '394b498e-1423-40fb-8ecc-b1ed7abbdd0c',
       'ab0e7e42-5172-46e5-bc30-50cedfdfaa10',
       '2b561dd5-7ba6-49af-a382-f83a3626e2ba',
       'bc0b4321-f3e3-4836-939b-4d54609d85e7',
       'c3f6f517-57aa-490a-8e36-e022849061ef',
       'b4ca781c-1f95-4022-82d9-602e4da50a2b',
       'dea74d01-c4af-41d4-8254-dcc8f048e35e',
       '41d0eb60-eedf-4f38-ae7d-ff236b0524bb',
       '63be7f73-c6c7-4793-b9cd-a5ea4cac7d8d',
       'e0fe0e73-9aaa-4e4e-9ee8-a29f749d7b0e',
       '882bfa2d-6b59-4d44-b4cb-8626805d2c12',
       '65388ae1-fa95-4d19-8360-9b98c49d3829',
       '251914d1-805b-46bd-bb0d-819f31ecf8ba',
       '863a6730-c9d5-4de1-be0a-832f8b25ef1a',
       '5de77b9e-cc31-4941-bfb7-5e03f1089db7',
       '1c1e8b59-9c24-4c28-9783-4583259c60ef',
       '9b0168d7-8d81-40b9-99aa-c3a0cadf1bd4',
       '470cb77f-d0a2-43e0-aa2d-2c6649eb96c9',
       '4609ba17-c412-47a5-8f78-95e3091d928c',
       '55a065b8-74b7-4ef9-9246-7008d5af2f95',
       '60f1c0a5-6850-4aa3-9f84-b0a5a181534c',
       'ddc6d737-77ab-4f48-b8bc-5a652b7ead4f',
       '95e9eee0-373b-4db7-9f84-8ee6b6b416d8',
       'acb5e302-4195-4cd7-8aac-d1e1c70b7b1e',
       '7c56eac7-9efc-40af-8592-72bc868da84b',
       'c6e70bce-3f49-4970-a9d5-0c49a4dad208',
       'f34be3ec-011c-4b82-9867-e76a963f9936',
       '65d6862b-1a39-4689-9c05-9520e3e7c848',
       'a585c17f-b1b8-4f6e-8e45-f7788e973c57',
       '9b5d9b68-e387-4cab-8281-923b32d6373b',
       'bf8f703f-e070-400c-9b32-da0830ec9bc4',
       '98e089d5-40af-4fc9-a104-835bbce48dd3',
       '9ba97032-07bd-4e95-8779-1650c3853789',
       'f95c2714-6ec2-4a25-af08-cfeab4ac05d1',
       'e727cd79-b95e-42cf-b67a-c07b91154f11',
       'b014f466-4d50-4673-bb1e-9e9c056dc132',
       '84e51c03-012a-4384-963c-bca0eab124b9',
       '598f4386-0272-4fa8-8536-0c1910d9e799',
       'cfc82cc9-5ca4-4ab7-bd13-b5c06c055c6a',
       '39351dae-df4c-430f-b1a6-aa96282c87cd',
       'c32be131-17e9-498c-ac66-dc706ce6be6a',
       '9e59ffaa-a9c5-4875-8fea-417ac518da2d',
       '6ce3c63f-c678-4121-8e94-ec7fcddf8e77',
       'f4d54fdf-9c53-43ef-bb12-1d21dc29ddd5',
       'ac22d243-e618-497b-95bc-40814f34f614',
       'afc22c96-4c97-407f-89d5-32df5087ff81',
       '0f3ef177-6632-4814-88a1-a98fe4b8e7f5',
       'd02032c9-13e3-42ee-be95-523f5d2e06d0',
       '1f9c11a7-09fc-44a0-a7f5-22f6ce819bcb',
       'fecbcebe-3634-495c-b0d4-a83caeb4666f',
       '9664cf77-4770-4c1f-ab20-cb44e6766a34',
       '3eaa8732-b945-418f-ba0c-40874ff3caf3',
       '4bff346b-87a8-4bcb-bd33-66f9e2339124',
       '72ed837d-c57a-4fe5-bfe7-c5620eb68680',
       '68e941f3-905a-4396-862d-efcb4198e17e',
       '467224c2-6ec0-454a-a7ba-1ca798fb0af9',
       '0088666a-d8dc-45b9-8bff-fb0809602feb',
       '348d74f7-a744-404e-939d-72f252725bb6',
       '0dda6ac7-7e9e-4d4f-8eb2-0447be7eeabf',
       'c9e08e44-4d29-47fc-b1e4-00ef2b40f787'
          )

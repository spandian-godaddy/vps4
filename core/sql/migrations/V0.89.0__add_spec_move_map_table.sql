CREATE TABLE vm_move_spec_map (
    id              SERIAL PRIMARY KEY,
    from_spec_id    SMALLINT NOT NULL REFERENCES virtual_machine_spec (spec_id),
    to_spec_id      SMALLINT NOT NULL REFERENCES virtual_machine_spec (spec_id)
);

--hosting.c1.r2.d40       oh.hosting.c1.r2.d40
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c1.r2.d40' and spec2.spec_name = 'oh.hosting.c1.r2.d40';
--hosting.c2.r4.d60       oh.hosting.c2.r4.d100
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c2.r4.d60' and spec2.spec_name = 'oh.hosting.c2.r4.d100';
--hosting.c3.r6.d90       oh.hosting.c3.r6.d150
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c3.r6.d90' and spec2.spec_name = 'oh.hosting.c3.r6.d150';
--hosting.c4.r8.d120      oh.hosting.c4.r8.d200
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c4.r8.d120' and spec2.spec_name = 'oh.hosting.c4.r8.d200';
--hosting.c1.r4.d40       oh.hosting.c1.r4.d40
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c1.r4.d40' and spec2.spec_name = 'oh.hosting.c1.r4.d40';
--hosting.c2.r8.d100      oh.hosting.c2.r8.d100
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c2.r8.d100' and spec2.spec_name = 'oh.hosting.c2.r8.d100';
--hosting.c4.r16.d200     oh.hosting.c4.r16.d200
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c4.r16.d200' and spec2.spec_name = 'oh.hosting.c4.r16.d200';
--hosting.c6.r16.d300     oh.hosting.c6.r16.d300
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c6.r16.d300' and spec2.spec_name = 'oh.hosting.c6.r16.d300';
--hosting.c6.r24.d300     oh.hosting.c6.r24.d300
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c6.r24.d300' and spec2.spec_name = 'oh.hosting.c6.r24.d300';
--hosting.c8.r16.d400     oh.hosting.c8.r16.d400
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c8.r16.d400' and spec2.spec_name = 'oh.hosting.c8.r16.d400';
--hosting.c8.r32.d400     oh.hosting.c8.r32.d400
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c8.r32.d400' and spec2.spec_name = 'oh.hosting.c8.r32.d400';
--hosting.c16.r32.d800    oh.hosting.c16.r32.d800
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c16.r32.d800' and spec2.spec_name = 'oh.hosting.c16.r32.d800';
--hosting.c16.r64.d800    oh.hosting.c16.r64.d800
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c16.r64.d800' and spec2.spec_name = 'oh.hosting.c16.r64.d800';
--hosting.c2.r4.d100      oh.hosting.c2.r4.d100
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c2.r4.d100' and spec2.spec_name = 'oh.hosting.c2.r4.d100';
--hosting.c3.r6.d150      oh.hosting.c3.r6.d150
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c3.r6.d150' and spec2.spec_name = 'oh.hosting.c3.r6.d150';
--hosting.c4.r8.d200      oh.hosting.c4.r8.d200
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c4.r8.d200' and spec2.spec_name = 'oh.hosting.c4.r8.d200';
--hosting.c1.r1.d20       oh.hosting.c1.r1.d20
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c1.r1.d20' and spec2.spec_name = 'oh.hosting.c1.r1.d20';
--hosting.c2.r8.d60       oh.hosting.c2.r8.d100
insert into vm_move_spec_map (from_spec_id, to_spec_id)
select spec1.spec_id, spec2.spec_id from virtual_machine_spec spec1, virtual_machine_spec spec2 where spec1.spec_name = 'hosting.c2.r8.d60' and spec2.spec_name = 'oh.hosting.c2.r8.d100';

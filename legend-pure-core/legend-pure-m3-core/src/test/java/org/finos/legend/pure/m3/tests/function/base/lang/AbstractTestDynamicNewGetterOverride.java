// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestDynamicNewGetterOverride extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimple()
    {
        compileTestSource("fromString.pure","""
                Enum myEnum{A,B}\
                Class A
                {
                   a: String[1];
                   b: String[0..1];
                   c: String[*];\
                   d : D[0..1];\
                   ds : D[*];\
                   enum : myEnum[1];
                   enums : myEnum[*];
                }\
                Class D\
                {\
                   name : String[1];\
                }
                
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                 \
                  let payload = $o->getHiddenPayload()->cast(@String)->toOne();
                 \
                  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());
                }
                
                function test():Any[*]
                {
                  let a = A;
                
                
                  let r = dynamicNew($a,
                                   [
                                      ^KeyValue(key='a',value='rrr'),
                                      ^KeyValue(key='b',value='eee'),
                                      ^KeyValue(key='c',value=['zzz','kkk']),
                                      ^KeyValue(key='enum',value=myEnum.A),
                                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2'
                                  )->cast(@A);
                
                   assert('2' == Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->toOne()->cast(@GetterOverride).hiddenPayload, |'');
                   assert('2' == $r.elementOverride->toOne()->cast(@GetterOverride).hiddenPayload, |'');
                   assert('2' == $r->getHiddenPayload(), |'');
                   // DataType [1]
                   assert('rrr' == $r.a, |'');
                   assert('rrr' == ^$r().a, |'');
                   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');
                   // DataType [0..1]
                   assert('eee' == $r.b, |'');
                   assert('eee' == ^$r().b, |'');
                   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');
                   // DataType [*]
                   assert(['zzz', 'kkk'] == $r.c, |'');
                   assert(['zzz', 'kkk'] == ^$r().c, |'');
                   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');
                   // Class [1]
                   assert('rrr2' == $r.d.name, |'');
                   assert('rrr2' == A->classPropertyByName('d')->toOne()->eval($r)->cast(@D).name, |'');
                   assert('rrr2' == ^$r().d.name, |'');
                   // Class [*]
                   assert(['rrr2','eee2'] == $r.ds.name, |'');
                   assert(['rrr2','eee2'] == A->classPropertyByName('ds')->toOne()->eval($r)->cast(@D).name, |'');
                   assert(['rrr2','eee2'] == ^$r().ds.name, |'');
                   // Enum [1]
                   assert(myEnum.A == $r.enum, |'');
                   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');
                   assert(myEnum.A == ^$r().enum, |'');
                   // Enum [*]
                   assert([myEnum.A,myEnum.B] == $r.enums, |'');
                   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');
                   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');
                }\
                """);
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testRemoveOverride()
    {
        compileTestSource("fromString.pure","""
                Enum myEnum{A,B}\
                Class A
                {
                   a: String[1];
                   b: String[0..1];
                   c: String[*];\
                   d : D[0..1];\
                   ds : D[*];\
                   enum : myEnum[1];
                   enums : myEnum[*];
                }\
                Class D\
                {\
                   name : String[1];\
                }
                
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                 \
                  let payload = $o->getHiddenPayload()->cast(@String)->toOne();
                 \
                  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());
                }
                
                function test():Any[*]
                {
                  let a = A;
                  let or = dynamicNew($a,
                                   [
                                      ^KeyValue(key='a',value='rrr'),
                                      ^KeyValue(key='b',value='eee'),
                                      ^KeyValue(key='c',value=['zzz','kkk']),
                                      ^KeyValue(key='enum',value=myEnum.A),
                                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2'
                                  )->cast(@A);
                   let r = $or->removeOverride();
                   assert(Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->isEmpty(), |'');
                   // DataType [1]
                   assert('rrr' == $r.a, |'');
                   assert('rrr' == ^$r().a, |'');
                   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');
                   // DataType [0..1]
                   assert('eee' == $r.b, |'');
                   assert('eee' == ^$r().b, |'');
                   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');
                   // DataType [*]
                   assert(['zzz', 'kkk'] == $r.c, |'');
                   assert(['zzz', 'kkk'] == ^$r().c, |'');
                   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');
                   // Class [1]
                   assert($r.d->isEmpty(), |'');
                   assert(A->classPropertyByName('d')->toOne()->eval($r)->isEmpty(), |'');
                   assert(^$r().d->isEmpty(), |'');
                   // Class [*]
                   assert($r.ds->isEmpty(), |'');
                   assert(A->classPropertyByName('ds')->toOne()->eval($r)->isEmpty(), |'');
                   assert(^$r().ds->isEmpty(), |'');
                   // Enum [1]
                   assert(myEnum.A == $r.enum, |'');
                   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');
                   assert(myEnum.A == ^$r().enum, |'');
                   // Enum [*]
                   assert([myEnum.A,myEnum.B] == $r.enums, |'');
                   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');
                   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');
                }\
                """);
        this.compileAndExecute("test():Any[*]");
    }


    @Test
    public void testRemoveOverrideWithMay()
    {
        compileTestSource("fromString.pure","""
                Enum myEnum{A,B}\
                Class A
                {
                   a: String[1];
                   b: String[0..1];
                   c: String[*];\
                   d : D[0..1];\
                   ds : D[*];\
                   enum : myEnum[1];
                   enums : myEnum[*];
                }\
                Class D\
                {\
                   name : String[1];\
                }
                
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                 \
                  let payload = $o->getHiddenPayload()->cast(@String)->toOne();
                 \
                  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());
                }
                
                function meta::pure::functions::lang::mayRemoveOverride<T>(value:T[0..1]):T[0..1]
                {
                   if ($value->isEmpty(),|$value,|$value->toOne()->removeOverride());
                }
                function test():Any[*]
                {
                  let a = A;
                  let or = dynamicNew($a,
                                   [
                                      ^KeyValue(key='a',value='rrr'),
                                      ^KeyValue(key='b',value='eee'),
                                      ^KeyValue(key='c',value=['zzz','kkk']),
                                      ^KeyValue(key='enum',value=myEnum.A),
                                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2'
                                  )->cast(@A);
                   let r = $or->mayRemoveOverride()->toOne();
                   assert(Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->isEmpty(), |'');
                   // DataType [1]
                   assert('rrr' == $r.a, |'');
                   assert('rrr' == ^$r().a, |'');
                   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');
                   // DataType [0..1]
                   assert('eee' == $r.b, |'');
                   assert('eee' == ^$r().b, |'');
                   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');
                   // DataType [*]
                   assert(['zzz', 'kkk'] == $r.c, |'');
                   assert(['zzz', 'kkk'] == ^$r().c, |'');
                   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');
                   // Class [1]
                   assert($r.d->isEmpty(), |'');
                   assert(A->classPropertyByName('d')->toOne()->eval($r)->isEmpty(), |'');
                   assert(^$r().d->isEmpty(), |'');
                   // Class [*]
                   assert($r.ds->isEmpty(), |'');
                   assert(A->classPropertyByName('ds')->toOne()->eval($r)->isEmpty(), |'');
                   assert(^$r().ds->isEmpty(), |'');
                   // Enum [1]
                   assert(myEnum.A == $r.enum, |'');
                   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');
                   assert(myEnum.A == ^$r().enum, |'');
                   // Enum [*]
                   assert([myEnum.A,myEnum.B] == $r.enums, |'');
                   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');
                   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');
                }\
                """);
        this.compileAndExecute("test():Any[*]");
    }
}

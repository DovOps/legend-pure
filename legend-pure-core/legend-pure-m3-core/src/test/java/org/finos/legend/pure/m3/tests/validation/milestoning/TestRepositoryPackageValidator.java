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

package org.finos.legend.pure.m3.tests.validation.milestoning;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestRepositoryPackageValidator extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
    }

    @Test
    public void testValidPackage1()
    {
        try
        {
            compileTestSource(
                    "testSource.pure",
                    """
                    function Root::hello():Any[*]
                    {
                       'hello';
                    }\
                    """
            );
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Root is not a valid user-defined package", "testSource.pure", 1, 1, 1, 16, 4, 1, e);
        }
    }

    @Test
    public void testValidPackage2()
    {
        try
        {
            compileTestSource(
                    "testSource.pure",
                    """
                    function Root::a::hello():Any[*]
                    {
                       'hello';
                    }\
                    """
            );
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Root is not a valid user-defined package", "testSource.pure", 1, 1, 1, 19, 4, 1, e);
        }
    }

    @Test
    public void testValidPackage3()
    {
        try
        {
            compileTestSource(
                    "testSource.pure",
                    """
                    function a::Root::hello():Any[*]
                    {
                       'hello';
                    }\
                    """
            );
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Root is not a valid user-defined package", "testSource.pure", 1, 1, 1, 19, 4, 1, e);
        }
    }

    @Test
    public void testValidPackage4()
    {
        try
        {
            compileTestSource(
                    "testSource.pure",
                    """
                    function a::Root::b::hello():Any[*]
                    {
                       'hello';
                    }\
                    """
            );
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Root is not a valid user-defined package", "testSource.pure", 1, 1, 1, 22, 4, 1, e);
        }
    }
}

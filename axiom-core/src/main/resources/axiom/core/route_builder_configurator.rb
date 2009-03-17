# Copyright (c) 2009, Tim Watson
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
#
#     * Redistributions of source code must retain the above copyright notice,
#       this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright notice,
#       this list of conditions and the following disclaimer in the documentation
#       and/or other materials provided with the distribution.
#     * Neither the name of the author nor the names of its contributors
#       may be used to endorse or promote products derived from this software
#       without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

require 'axiom'
require 'axiom/core/route_builder'
require 'axiom/core/configuration'
require 'axiom/plugins'

module Axiom
  module Core

    # provides a mechanism for evaluating a script (source) in the
    # context of the current JRuby runtime (which is nigh on impossible to
    # get out of spring otherwise - creating a second runtime is semantically
    # incorrect), and having the result evaluated as a block passed to RouteBuider
    class RouteBuilderConfigurator
      include org.axiom.integration.camel.RouteConfigurationScriptEvaluator
      include Configuration
      include Axiom::Plugins

      attr_accessor :camel_context
      alias setCamelContext camel_context=    # does jruby do this for us?

      # convenience hook for script writers
      def route &block
        logger.debug "Generating route builder from block."
        builder = SimpleRouteBuilder.new &block
        builder.properties = self.properties
        builder.context = self.camel_context
        builder
      end

      # configures the supplied script source in the context of a RouteBuilder instance
      def configure script_body
        logger.debug "Evaluating configuration script."
        response = instance_eval script_body
        logger.debug "Script evaluated to #{response}."
        response
      end

    end

  end
end

# This return value (for the script) is a hook for spring-framework integration
Axiom::Core::RouteBuilderConfigurator.new

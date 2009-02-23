=begin
Copyright (c) 2009, Tim Watson
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the author nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
=end

require 'ruby/functor'
require 'ruby/processor'

module Axiom

  # wraps the camel RouteBuilder and evaluates a block of
  # route configuration code in the instance context (thereby
  # providing a convenient and simpilfied syntax for defining
  # RouteBuilder instances without messy java noise)
  class SimpleRouteBuilder < org.apache.camel.builder.RouteBuilder
    include Axiom::Functor

    # adds all the header k=>v pairs from the supplied hash
    # to the current route

    def add_header hash
      Axiom::Processor.new do |exchange|
        out_channel = exchange.out
        hash.each { |k, v| out_channel.set_header k, v }
      end
    end

    # see the javadoc for org.apache.camel.RouteBuilder

    def configure
      instance_eval &self
    end
  end

end

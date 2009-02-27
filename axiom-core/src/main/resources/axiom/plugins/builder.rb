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


module Axiom
  module Plugins

    def self.plugin name, &block
      self.send :define_method, name, &block
    end

    def plugin name, &block
      Axiom::Plugins.plugin name, &block
    end

    # TODO: implement load_plugin to do a *sensible* classpath search and require...

    def register_plugin name, clazz
      fail_registration(clazz) unless clazz.respond_to? :new
      plugin(name) { |*args| clazz.send(:new, *args) }
    end

    def lookup_plugin name, plugin_id
      plugin(name) do |*args|
        fail_properties(args) unless args.size <= 1
        thing = context.registry.lookup plugin_id
        (args.first || {}).each do |k,v|
          property = "#{k}=".to_sym
          fail_property_assignment property, thing unless thing.respond_to? property
          thing.send(property, v)
        end
      end
    end

    private

    def fail_property_assignment property, thing
      fail "Property #{property} is not supported by #{thing}."
    end

    def fail_registration clazz
      fail "Cannot register class #{clazz} as a plugin because it has no public constructor!"
    end

    def fail_properties args
      raise ArgumentError.new "Only named args supported: supplied #{args}"
    end

  end
end
